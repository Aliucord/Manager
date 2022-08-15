package com.aliucord.manager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.domain.manager.DownloadManager
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.installer.service.InstallService
import com.aliucord.manager.installer.util.ManifestPatcher
import com.aliucord.manager.installer.util.Signer
import com.android.zipflinger.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.zip.Deflater

class InstallViewModel(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesManager
) : ViewModel() {
    private val assetManager = application.assets
    private val externalCacheDir = application.externalCacheDir!!
    private val packageInstaller = application.packageManager.packageInstaller

    private val _returnToHome = MutableSharedFlow<Boolean>()
    val returnToHome = _returnToHome.asSharedFlow()

    private var installationRunning: Boolean = false

    var log by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            startInstallation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun startInstallation() {
        if (installationRunning) return

        withContext(Dispatchers.IO) {
            installationRunning = true

            val arch = Build.SUPPORTED_ABIS.first()
            val supportedVersion = preferences.version

            val baseApk = externalCacheDir.resolve("base-${supportedVersion}.apk").also { file ->
                @Suppress("DEPRECATION")
                val archiveInfo = application.packageManager.getPackageArchiveInfo(file.path, 0)

                if (archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@also run { log += "Using cached base APK\n" }

                log += "Downloading Discord APK... "
                downloadManager.downloadDiscordApk(supportedVersion)
                log += "Done\n"
            }

            val libArch = arch.replace("-v", "_v")
            val libsApk = externalCacheDir.resolve("config.$libArch-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached libs APK\n" }

                log += "Downloading libs APK... "
                downloadManager.downloadSplit(supportedVersion, "config.$libArch")
                log += "Done\n"
            }

            val localeApk = externalCacheDir.resolve("config.en-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached locale APK\n" }

                log += "Downloading locale APK... "
                downloadManager.downloadSplit(supportedVersion, "config.en")
                log += "Done\n"
            }

            val xxhdpiApk = externalCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached drawables APK\n" }

                log += "Downloading drawables APK... "
                downloadManager.downloadSplit(supportedVersion, "config.xxhdpi")
                log += "Done\n"
            }

            val apks = arrayOf(baseApk, libsApk, localeApk, xxhdpiApk)

            log += "Patching manifest...\n"

            try {
                val byteArray = ZipRepo(baseApk.absolutePath)
                    .use { zip -> zip.getContent("AndroidManifest.xml") }
                    .array()

                ZipArchive(baseApk.toPath()).use { zip ->
                    val patchedBytesArray = ManifestPatcher.patchManifest(
                        manifestBytes = byteArray,
                        packageName = preferences.packageName,
                        appName = preferences.appName,
                        debuggable = preferences.debuggable,
                    )

                    val source = BytesSource(patchedBytesArray, "AndroidManifest.xml", Deflater.DEFAULT_COMPRESSION)

                    zip.delete("AndroidManifest.xml")
                    zip.add(source)
                }

                apks.forEach { apk ->
                    val byteArray = ZipRepo(apk.absolutePath)
                        .use { zip -> zip.getContent("AndroidManifest.xml") }
                        .array()

                    ZipArchive(apk.toPath()).use { zip ->
                        val patchedBytes = ManifestPatcher.renamePackage(byteArray, preferences.packageName)
                        val source = BytesSource(patchedBytes, "AndroidManifest.xml", Deflater.DEFAULT_COMPRESSION)

                        zip.delete("AndroidManifest.xml")
                        zip.add(source)
                    }
                }
            } catch (e: Exception) {
                log += "An error has occurred: ${e.message}\n"
                e.printStackTrace()

                return@withContext
            }

            log += "Patching libraries...\n"

            ZipArchive(libsApk.toPath()).use { zip ->
                val libs = listOf("libhermes.so", "libc++_shared.so")

                libs.forEach { lib -> zip.delete("lib/$arch/$lib") }
                libs.forEach { lib ->
                    val source = BytesSource(assetManager.open("lib/$arch/$lib"), "lib/$arch/$lib", Deflater.NO_COMPRESSION).apply {
                        align(4096)
                    }

                    zip.add(source)
                }
            }

            log += "Signing APKs... "

            apks.forEach(Signer::signApk)

            log += "Done\n"

            installApks(*apks)

            _returnToHome.emit(true)
            installationRunning = false
        }
    }

    private fun installApks(vararg apks: File) {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        apks.forEach { apk ->
            session.openWrite(apk.name, 0, apk.length()).use {
                it.write(apk.readBytes())

                session.fsync(it)
            }
        }

        val callbackIntent = Intent(application, InstallService::class.java)

        @SuppressLint("UnspecifiedImmutableFlag")
        val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(application, 0, callbackIntent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getService(application, 0, callbackIntent, 0)
        }

        session.commit(contentIntent.intentSender)
        session.close()
    }
}
