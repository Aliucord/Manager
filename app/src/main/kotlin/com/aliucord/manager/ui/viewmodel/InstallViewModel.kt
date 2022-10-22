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
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.ui.screen.InstallData
import com.github.diamondminer88.zip.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.time.Instant

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
            val latestHermesRelease = githubRepository.getHermesReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

            // Download the hermes-release.aar file to replace in the apk
            val hermesLibrary = externalCacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also run { log += "Using cached patched hermes library\n" }

                log += "Downloading patched hermes library... "
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
            val latestAliucordNativeRelease = githubRepository.getAliucordNativeReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

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

                ZipWriter(baseApkFile, true).use { baseApk ->
                    val mipmaps = arrayOf("mipmap-xhdpi-v4", "mipmap-xxhdpi-v4", "mipmap-xxxhdpi-v4")
                    val icons = arrayOf("ic_logo_foreground.png", "ic_logo_square.png", "ic_logo_foreground.png")

                    for (icon in icons) {
                        val newIcon = application.assets.open("icons/$icon")
                            .use { it.readBytes() }

                        for (mipmap in mipmaps) {
                            val path = "res/$mipmap/$icon"
                            baseApk.deleteEntry(path)
                            baseApk.writeEntry(path, newIcon)
                        }
                    }
                }

                log += "Done\n"
            }

            log += "Patching manifests... "
            try {
                apks.forEach { apk ->
                    val manifest = ZipReader(apk)
                        .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
                        ?: throw IllegalStateException("No manifest in ${apk.name}")

                    ZipWriter(apk, true).use { zip ->
                        val patchedManifestBytes = if (apk == baseApkFile) {
                            ManifestPatcher.patchManifest(
                                manifestBytes = manifest,
                                packageName = preferences.packageName,
                                appName = preferences.appName,
                                debuggable = preferences.debuggable,
                            )
                        } else {
                            ManifestPatcher.renamePackage(manifest, preferences.packageName)
                        }

                        zip.deleteEntry("AndroidManifest.xml", apk == libsApkFile) // Preserve alignment in libs apk
                        zip.writeEntry("AndroidManifest.xml", patchedManifestBytes)
                    }
                }
            } catch (e: Exception) {
                log += "An error has occurred: ${e.message}\n"
                e.printStackTrace()

                return@withContext
            }
            log += "Done\n"

            log += "Patching java classes... "

            val (dexCount, firstDexBytes) = ZipReader(baseApkFile).use { zip ->
                Pair(
                    // Find the amount of .dex files in apk
                    zip.entryNames.count { it.endsWith(".dex") },

                    // Get the first classes.dex bytes
                    zip.openEntry("classes.dex")?.read()
                        ?: throw IllegalStateException("No classes.dex in base apk")
                )
            }

            ZipWriter(baseApkFile, true).use { zip ->
                // Move first classes.dex to the dex file count + 1 to make place for Aliucord's .dex
                zip.deleteEntry("classes.dex")
                zip.writeEntry("classes${dexCount + 1}.dex", firstDexBytes)

                // Add Aliucord's .dex and make it load first by being the first .dex
                zip.writeEntry("classes.dex", aliucordDexFile.readBytes())
            }
            log += "Done\n"

            log += "Patching libraries... "

            ZipWriter(libsApkFile, true).use { libsApk ->
                // Process the hermes and cpp runtime library
                for (libFile in arrayOf(hermesLibrary, cppRuntimeLibrary)) {
                    // Map .aar to the embedded .so inside
                    val binaryName = with(libFile.name) {
                        when {
                            startsWith("hermes-release") -> "libhermes.so"
                            startsWith("hermes-cppruntime-release") -> "libc++_shared.so"
                            else -> throw Error("Unable to map $this to embedded .so")
                        }
                    }

                    // Read the embedded .so inside the .aar library
                    val libBytes = ZipReader(libFile).use { libZip ->
                        libZip.openEntry("jni/$arch/$binaryName")?.read()
                            ?: throw IllegalStateException("Failed to read jni/$arch/$binaryName from ${libFile.name}")
                    }

                    // Delete the old binary and add the new one instead
                    libsApk.deleteEntry("lib/$arch/$binaryName", true)
                    libsApk.writeEntry("lib/$arch/$binaryName", libBytes, ZipCompression.NONE, 4096)
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
