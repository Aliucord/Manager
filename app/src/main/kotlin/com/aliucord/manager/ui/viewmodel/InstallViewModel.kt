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
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.service.InstallService
import com.aliucord.manager.installer.util.ManifestPatcher
import com.aliucord.manager.installer.util.Signer
import com.aliucord.manager.ui.screen.InstallData
import com.android.zipflinger.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.time.Instant
import java.util.zip.Deflater

class InstallViewModel(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesManager,
    private val githubRepository: GithubRepository,
    private val installData: InstallData
) : ViewModel() {
    private val externalCacheDir = application.externalCacheDir!!
    private val filesDir = application.filesDir!!
    private val packageInstaller = application.packageManager.packageInstaller

    private val dexRegex = Regex("classes(\\d)?\\.dex")

    private val _returnToHome = MutableSharedFlow<Boolean>()
    val returnToHome = _returnToHome.asSharedFlow()

    private var installationRunning: Boolean = false

    var log by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.Main) {
            startInstallation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun startInstallation() {
        if (installationRunning) return

        withContext(Dispatchers.IO) {
            installationRunning = true

            externalCacheDir.resolve("patched").runCatching { deleteRecursively() }

            val arch = Build.SUPPORTED_ABIS.first()
            val supportedVersion = preferences.version

            // Download base.apk
            val baseApkFile = externalCacheDir.resolve("base-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    log += "Using cached base APK\n"
                } else {
                    log += "Downloading Discord APK... "
                    downloadManager.downloadDiscordApk(supportedVersion)
                    log += "Done\n"
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }

            // Download the native libraries split
            val libArch = arch.replace("-v", "_v")
            val libsApkFile = externalCacheDir.resolve("config.$libArch-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    log += "Using cached libs APK\n"
                } else {
                    log += "Downloading libs APK... "
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.$libArch"
                    )
                    log += "Done\n"
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }

            // Download the locale split
            val localeApkFile = externalCacheDir.resolve("config.en-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    log += "Using cached locale APK\n"
                } else {
                    log += "Downloading locale APK... "
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.en"
                    )
                    log += "Done\n"
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }

            // Download the drawables split
            val xxhdpiApkFile = externalCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    log += "Using cached drawables APK\n"
                } else {
                    log += "Downloading drawables APK... "
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.xxhdpi"
                    )
                    log += "Done\n"
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }

            // Fetch gh releases for Aliucord/Hermes
            val latestHermesRelease = githubRepository
                .getHermesReleases().reduce { current, new ->
                    if (Instant.parse(current.createdAt).isBefore(Instant.parse(new.createdAt))) new else current
                }

            // Download the hermes-release.aar file to replace in the apk
            val hermesLibrary = externalCacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also run { log += "Using cached patched hermes library\n" }

                log += "Downloading cached patched hermes library... "
                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-release-${latestHermesRelease.tagName}.aar"
                )
                log += "Done\n"
            }

            // Download the hermes-cppruntime-release.aar file to replace in the apk
            val cppRuntimeLibrary = externalCacheDir.resolve("hermes-cppruntime-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also run { log += "Using cached patched c++ runtime library\n" }

                log += "Downloading patched c++ runtime library... "
                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-cppruntime-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-cppruntime-release-${latestHermesRelease.tagName}.aar"
                )
                log += "Done\n"
            }

            // Fetch the gh releases for Aliucord/AliucordNative
            val latestAliucordNativeRelease = githubRepository
                .getAliucordNativeReleases().reduce { current, new ->
                    if (Instant.parse(current.createdAt).isBefore(Instant.parse(new.createdAt))) {
                        new
                    } else {
                        current
                    }
                }

            // Download the Aliucord classes.dex file to add to the apk
            val aliucordDexFile = externalCacheDir.resolve("classes-${latestAliucordNativeRelease.tagName}.dex").also { file ->
                if (file.exists()) return@also run { log += "Using cached Aliucord dex file\n" }

                log += "Downloading Aliucord dex file... "
                downloadManager.download(
                    url = latestAliucordNativeRelease.assets.find { it.name == "classes.dex" }!!.browserDownloadUrl,
                    fileName = "classes-${latestAliucordNativeRelease.tagName}.dex"
                ).apply {
                    copyTo(
                        externalCacheDir
                            .resolve("patched")
                            .resolve(this.name),
                        true
                    )
                }.also { log += "Done\n" }
            }

            val apks = arrayOf(baseApkFile, libsApkFile, localeApkFile, xxhdpiApkFile)

            if (preferences.replaceIcon) {
                log += "Replacing App Icons... "

                ZipArchive(baseApkFile.toPath()).use { baseApk ->
                    val mipmaps = arrayOf("mipmap-xhdpi-v4", "mipmap-xxhdpi-v4", "mipmap-xxxhdpi-v4")
                    val icons = arrayOf("ic_logo_foreground.png", "ic_logo_square.png", "ic_logo_foreground.png")

                    for (icon in icons) {
                        val newIcon = application.assets.open("icons/$icon")
                            .use { it.readBytes() }

                        for (mipmap in mipmaps) {
                            val path = "res/$mipmap/$icon"
                            baseApk.delete(path)
                            baseApk.add(BytesSource(newIcon, path, Deflater.DEFAULT_COMPRESSION))
                        }
                    }
                }

                log += "Done\n"
            }

            log += "Patching manifest... "

            try {
                val byteArray = ZipRepo(baseApkFile.absolutePath)
                    .use { zip -> zip.getContent("AndroidManifest.xml") }
                    .array()

                ZipArchive(baseApkFile.toPath()).use { zip ->
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
            log += "Done\n"

            log += "Patching java classes... "

            ZipArchive(baseApkFile.toPath()).use { baseApk ->
                // List all files in the base apk, in order to increment the ordinals of all dex files
                baseApk.listEntries().mapNotNull {
                    // Check if the file is a dex file, if not ignore it by returning null
                    val match = dexRegex.matchEntire(it) ?: return@mapNotNull null
                    // Return a Pair<Int, String> with the ordinal of the classes.dex file and the actual filename
                    (match.groups[1]?.value?.toInt() ?: 1) to it
                }.sortedByDescending { (ordinal) ->
                    // Sort the dex files by ordinal (descending) so they can be renamed without any conflicts
                    ordinal
                }.forEach { (ordinal, fileName) ->
                    println("Moving $fileName ($ordinal) to classes${ordinal + 1}.dex")
                    // Read the dex file
                    val inputStream = baseApk.getInputStream(fileName)
                    // Construct a new BytesSource with the same data, but incrementing
                    // the ordinal by one to make room for the Aliucord classes.dex
                    val incrementedDex = BytesSource(inputStream, "classes${ordinal + 1}.dex", Deflater.DEFAULT_COMPRESSION)
                    // Add the incremented dex file to the apk
                    baseApk.add(incrementedDex)
                    // Remove the original dex file
                    baseApk.delete(fileName)
                }
                // Add the Aliucord classes.dex file
                baseApk.add(
                    BytesSource(aliucordDexFile.inputStream(), "classes.dex", Deflater.DEFAULT_COMPRESSION)
                )
            }
            log += "Done\n"

            log += "Patching libraries... "

            ZipArchive(libsApkFile.toPath()).use { libsApk ->
                // Loop over the aar files
                listOf(hermesLibrary, cppRuntimeLibrary).forEach {
                    // Open each aar file as a zip file
                    ZipArchive(it.toPath()).use { aar ->
                        // Map the aar name to the library name
                        val lib = with(it.name) {
                            when {
                                startsWith("hermes-release") -> "libhermes.so"
                                startsWith("hermes-cppruntime-release") -> "libc++_shared.so"
                                else -> throw Error("Unable to map .aar name to .so name")
                            }
                        }
                        // Delete existing library
                        libsApk.delete("lib/$arch/$lib")
                        // Add new library
                        val source = BytesSource(aar.getInputStream("jni/$arch/$lib"), "lib/$arch/$lib", Deflater.NO_COMPRESSION).apply {
                            align(4096)
                        }
                        libsApk.add(source)
                    }
                }
            }
            log += "Done\n"
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
