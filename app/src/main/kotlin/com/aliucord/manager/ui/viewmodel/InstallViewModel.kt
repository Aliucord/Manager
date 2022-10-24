package com.aliucord.manager.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.domain.manager.DownloadManager
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.ui.dialog.DiscordType
import com.aliucord.manager.ui.screen.InstallData
import com.github.diamondminer88.zip.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class InstallViewModel(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesManager,
    private val githubRepository: GithubRepository,
    private val aliucordMaven: AliucordMavenRepository,
    private val installData: InstallData
) : ViewModel() {
    private val externalCacheDir = application.externalCacheDir!!

    private val installationRunning = AtomicBoolean(false)
    private var installStep = InstallStep.NONE
    private var elapsedTime = 0f

    private val _returnToHome = MutableSharedFlow<Boolean>()
    val returnToHome = _returnToHome.asSharedFlow()

    var log by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.Main) {
            if (installationRunning.getAndSet(true)) {
                return@launch
            }

            log += "Aliucord Manager ${BuildConfig.VERSION_NAME}\n"
            log += "Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH}\n"
            if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) {
                log += "Local commits or changes are present!\n"
            }
            log += "Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}\n"
            log += "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}\n\n"
            log += "Installing ${installData.discordType} with the ${installData.downloadMethod} apk method\n"

            withContext(Dispatchers.IO) {
                try {
                    when (installData.discordType) {
                        DiscordType.REACT_NATIVE -> installReactNative()
                        DiscordType.KOTLIN -> installKotlin()
                    }
                    _returnToHome.emit(true)
                } catch (t: Throwable) {
                    Log.e(
                        BuildConfig.TAG,
                        "Failed to patch ${installData.discordType.name} during ${installStep.name}: ${Log.getStackTraceString(t)}"
                    )
                }

                installationRunning.set(false)
            }
        }
    }

    private suspend fun installReactNative() {
        externalCacheDir.resolve("patched").runCatching { deleteRecursively() }

        val arch = Build.SUPPORTED_ABIS.first()
        val supportedVersion = preferences.version

        // Download base.apk
        val baseApkFile = step(InstallStep.DL_BASE_APK) {
            externalCacheDir.resolve("base-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    log += "cached... "
                } else {
                    downloadManager.downloadDiscordApk(supportedVersion)
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the native libraries split
        val libsApkFile = step(InstallStep.DL_LIBS_APK) {
            val libArch = arch.replace("-v", "_v")
            externalCacheDir.resolve("config.$libArch-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    log += "cached... "
                } else {
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.$libArch"
                    )
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the locale split
        val localeApkFile = step(InstallStep.DL_LOCALES_APKS) {
            externalCacheDir.resolve("config.en-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    log += "cached... "
                } else {
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.en"
                    )
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the drawables split
        val resApkFile = step(InstallStep.DL_RES_APKS) {
            // TODO: download the appropriate dpi res apk
            externalCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    log += "cached... "
                } else {
                    downloadManager.downloadSplit(
                        version = supportedVersion,
                        split = "config.xxhdpi"
                    )
                }

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download hermes & cppruntime lib
        val (hermesLibrary, cppRuntimeLibrary) = step(InstallStep.DL_HERMES) {
            // Fetch gh releases for Aliucord/Hermes
            val latestHermesRelease = githubRepository.getHermesReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

            // Download the hermes-release.aar file to replace in the apk
            val hermes = externalCacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-release-${latestHermesRelease.tagName}.aar"
                )
            }

            // Download the hermes-cppruntime-release.aar file to replace in the apk
            val cppruntime = externalCacheDir.resolve("hermes-cppruntime-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-cppruntime-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-cppruntime-release-${latestHermesRelease.tagName}.aar"
                )
            }

            Pair(hermes, cppruntime)
        }

        // Download Aliucord Native lib
        val aliucordDexFile = step(InstallStep.DL_ALIUNATIVE) {
            // Fetch the gh releases for Aliucord/AliucordNative
            val latestAliucordNativeRelease = githubRepository.getAliucordNativeReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

            // Download the Aliucord classes.dex file to add to the apk
            externalCacheDir.resolve("classes-${latestAliucordNativeRelease.tagName}.dex").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

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
                }
            }
        }

        val apks = arrayOf(baseApkFile, libsApkFile, localeApkFile, resApkFile)

        // Replace app icons
        if (preferences.replaceIcon) {
            step(InstallStep.APP_ICONS) {
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
            }
        }

        // Patch manifests
        step(InstallStep.MANIFESTS) {
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
        }

        // Re-order dex files
        step(InstallStep.DEX) {
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
        }

        // Replace libs
        step(InstallStep.REPLACE_LIBS) {
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
        }

        step(InstallStep.SIGNING) {
            apks.forEach(Signer::signApk)
        }

        step(InstallStep.INSTALLING) {
            application.packageManager.packageInstaller
                .installApks(application, *apks)
        }

        log += "\nCompleted in %.2f seconds".format(elapsedTime)
    }

    private suspend fun installKotlin() {
        val dataJson = step(InstallStep.KT_FETCH_VERSION) {
            githubRepository.getDiscordKtVersion().getOrThrow()
        }

        val arch = Build.SUPPORTED_ABIS.first()
        val cacheDir = externalCacheDir
        val discordCacheDir = externalCacheDir.resolve(dataJson.versionCode)
        val patchedDir = discordCacheDir.resolve("patched").also { it.deleteRecursively() }

        // Download base.apk
        val baseApkFile = step(InstallStep.DL_KT_APK) {
            discordCacheDir.resolve("base.apk").let { file ->
                if (file.exists()) {
                    log += "cached... "
                } else {
                    downloadManager.downloadDiscordApk(dataJson.versionCode, file)
                }

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        // Download Aliuhook aar
        val aliuhookAarFile = step(InstallStep.DL_ALIUHOOK) {
            // Fetch aliuhook version
            val aliuhookVersion = aliucordMaven.getAliuhookVersion().getOrThrow()

            // Download aliuhook aar
            cacheDir.resolve("aliuhook-${aliuhookVersion}.aar").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

                downloadManager.downloadAliuhook(aliuhookVersion, file)
            }
        }

        // Download the injector dex
        val injectorFile = step(InstallStep.DL_INJECTOR) {
            cacheDir.resolve("injector-${dataJson.aliucordHash}.dex").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

                downloadManager.downloadKtInjector(file)
            }
        }

        val kotlinFile = step(InstallStep.DL_KOTLIN) {
            cacheDir.resolve("kotlin.dex").also { file ->
                if (file.exists()) {
                    log += "cached... "
                    return@also
                }

                downloadManager.downloadKotlinDex(file)
            }
        }

        // Replace app icons
        if (preferences.replaceIcon) {
            step(InstallStep.APP_ICONS) {
                ZipWriter(baseApkFile, true).use { baseApk ->
                    val foregroundIcon = application.assets.open("icons/ic_logo_foreground.png")
                        .use { it.readBytes() }
                    val squareIcon = application.assets.open("icons/ic_logo_square.png")
                        .use { it.readBytes() }

                    val replacements = mapOf(
                        arrayOf("MbV.png", "kbF.png", "_eu.png", "EtS.png") to foregroundIcon,
                        arrayOf("_h_.png", "9MB.png", "Dy7.png", "kC0.png", "oEH.png", "RG0.png", "ud_.png", "W_3.png") to squareIcon
                    )

                    for ((files, replacement) in replacements) {
                        for (file in files) {
                            val path = "res/$file"
                            baseApk.deleteEntry(path)
                            baseApk.writeEntry(path, replacement)
                        }
                    }
                }
            }
        }

        // Patch manifests
        step(InstallStep.MANIFESTS) {
            val manifest = ZipReader(baseApkFile)
                .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
                ?: throw IllegalStateException("No manifest in base apk")

            ZipWriter(baseApkFile, true).use { zip ->
                val patchedManifestBytes = ManifestPatcher.patchManifest(
                    manifestBytes = manifest,
                    packageName = preferences.packageName,
                    appName = preferences.appName,
                    debuggable = preferences.debuggable,
                )

                zip.deleteEntry("AndroidManifest.xml")
                zip.writeEntry("AndroidManifest.xml", patchedManifestBytes)
            }
        }

        // Re-order dex files
        step(InstallStep.DEX) {
            val (dexCount, firstDexBytes) = ZipReader(baseApkFile).use { zip ->
                Pair(
                    // Find the amount of .dex files in apk
                    zip.entryNames.count { it.endsWith(".dex") },

                    // Get the first dex
                    zip.openEntry("classes.dex")?.read()
                        ?: throw IllegalStateException("No classes.dex in base apk")
                )
            }

            ZipWriter(baseApkFile, true).use { zip ->
                // Move copied dex to end of dex list
                zip.deleteEntry("classes.dex")
                zip.writeEntry("classes${dexCount + 1}.dex", firstDexBytes)

                // Add Kotlin & Aliucord's dex
                zip.writeEntry("classes.dex", injectorFile.readBytes())
                zip.writeEntry("classes${dexCount + 2}.dex", kotlinFile.readBytes())
                zip.writeEntry("classes${dexCount + 3}.dex", application.assets.open("aliuhook.dex").use { it.readBytes() })
            }
        }

        // Replace libs
        step(InstallStep.REPLACE_LIBS) {
            ZipWriter(baseApkFile, true).use { baseApk ->
                ZipReader(aliuhookAarFile).use { aliuhookAar ->
                    for (libFile in arrayOf("libaliuhook.so", "libc++_shared.so", "liblsplant.so")) {
                        val bytes = aliuhookAar.openEntry("jni/$arch/$libFile")?.read()
                            ?: throw IllegalStateException("Failed to read $libFile from aliuhook aar")

                        baseApk.writeEntry("lib/$arch/$libFile", bytes)
                    }
                }

                // TODO: Add aliuhook's dex file
            }
        }

        step(InstallStep.SIGNING) {
            Signer.signApk(baseApkFile)
        }

        step(InstallStep.INSTALLING) {
            application.packageManager.packageInstaller
                .installApks(application, baseApkFile)
        }

        // patchedDir.deleteRecursively()

        log += "\nCompleted in %.2f seconds".format(elapsedTime)
    }

    @OptIn(ExperimentalTime::class)
    private inline fun <T> step(step: InstallStep, block: () -> T): T {
        log += "${step.log}... "
        installStep = step

        try {
            val value = measureTimedValue(block)
            val time = value.duration.inWholeMilliseconds.div(1000f)

            elapsedTime += time
            log += "Done in %.2fs\n".format(time)

            return value.value
        } catch (t: Throwable) {
            val stacktrace = Log.getStackTraceString(t)

            log += "\n$stacktrace"
            log += "\nFailed to install Aliucord ${installData.discordType.name} during the ${installStep.name} step!"

            throw t
        }
    }

    private enum class InstallStep(val log: String) {
        // Shared install steps
        NONE(""),
        APP_ICONS("Patching app icons"),
        MANIFESTS("Patching apk manifests"),
        DEX("Adding Aliucord dex into apk"),
        REPLACE_LIBS("Replacing libraries"),
        SIGNING("Signing apks"),
        INSTALLING("Installing apks"),

        // Kotlin exclusive install steps
        KT_FETCH_VERSION("Fetching supported version"),
        DL_KT_APK("Downloading Discord apk"),
        DL_KOTLIN("Downloading Kotlin dex"),
        DL_INJECTOR("Downloading Aliucord injector"),
        DL_ALIUHOOK("Downloading Aliuhook library"),

        // AliuRN exclusive install steps
        DL_BASE_APK("Downloading base apk"),
        DL_LIBS_APK("Downloading libraries apk"),
        DL_LOCALES_APKS("Downloading locale apks"),
        DL_RES_APKS("Downloading resource apk"),
        DL_HERMES("Downloading patched hermes & c++ runtime library"),
        DL_ALIUNATIVE("Downloading AliucordNative library"),
    }
}
