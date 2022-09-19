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
import com.aliucord.manager.network.service.GithubService
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

            val arch = Build.SUPPORTED_ABIS.first()
            val supportedVersion = preferences.version

            val baseApkFile = externalCacheDir.resolve("base-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached base APK\n" }

                log += "Downloading Discord APK... "
                downloadManager.downloadDiscordApk(supportedVersion)
                log += "Done\n"
            }

            val libArch = arch.replace("-v", "_v")
            val libsApkFile = externalCacheDir.resolve("config.$libArch-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached libs APK\n" }

                log += "Downloading libs APK... "
                downloadManager.downloadSplit(supportedVersion, "config.$libArch")
                log += "Done\n"
            }

            val localeApkFile = externalCacheDir.resolve("config.en-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached locale APK\n" }

                log += "Downloading locale APK... "
                downloadManager.downloadSplit(supportedVersion, "config.en")
                log += "Done\n"
            }

            val xxhdpiApkFile = externalCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also run { log += "Using cached drawables APK\n" }

                log += "Downloading drawables APK... "
                downloadManager.downloadSplit(supportedVersion, "config.xxhdpi")
                log += "Done\n"
            }

            val (hermesLibrary, cppRuntimeLibrary) = run {
                val latestHermesRelease = githubRepository
                    .getReleases(GithubService.AliucordRepo.HERMES).reduce { current, new ->
                        if (Instant.parse(current.createdAt).isBefore(Instant.parse(new.createdAt))) new else current
                    }
                val hermesFile = externalCacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar")
                val cppRuntimeFile = externalCacheDir.resolve("hermes-cppruntime-release-${latestHermesRelease.tagName}.aar")
                if (
                    hermesFile.exists()
                    && cppRuntimeFile.exists()
                ) (hermesFile to cppRuntimeFile).also { log += "Using cached patched libraries\n" }
                else {
                    log += "Downloading patched libraries... "

                    (
                        downloadManager.download(
                            url = latestHermesRelease.assets.find { it.name == "hermes-release.aar" }!!.browserDownloadUrl,
                            fileName = hermesFile.name
                        ) to downloadManager.download(
                            url = latestHermesRelease.assets.find { it.name == "hermes-cppruntime-release.aar" }!!.browserDownloadUrl,
                            fileName = cppRuntimeFile.name
                        )
                    ).also { log += "Done\n" }
                }
            }

            val latestAliucordNativeRelease = githubRepository
                .getReleases(GithubService.AliucordRepo.ALIUCORD_NATIVE).reduce { current, new ->
                    if (Instant.parse(current.createdAt).isBefore(Instant.parse(new.createdAt))) new else current
                }

            val aliucordDexFile = externalCacheDir.resolve("classes-${latestAliucordNativeRelease.tagName}.dex").also { file ->
                if (file.exists()) return@also run { log += "Using cached Aliucord dex file\n" }

                log += "Downloading Aliucord dex file... "
                downloadManager.download(
                    url = latestAliucordNativeRelease.assets.find { it.name == "classes.dex" }!!.browserDownloadUrl,
                    fileName = "classes-${latestAliucordNativeRelease.tagName}.dex"
                ).also { log += "Done\n" }
            }

            val apks = arrayOf(baseApkFile, libsApkFile, localeApkFile, xxhdpiApkFile)

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
                    (
                        match.groups[1]?.value?.toInt() ?: 1
                    ) to it
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
