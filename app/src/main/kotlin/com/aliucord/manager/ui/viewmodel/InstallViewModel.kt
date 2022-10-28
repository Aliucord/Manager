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
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.ui.component.installer.Status
import com.aliucord.manager.ui.component.installer.Step
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
    val preferences: PreferencesManager,
    private val githubRepository: GithubRepository,
    private val installData: InstallData
) : ViewModel() {
    private val externalCacheDir = application.externalCacheDir!!

    private val installationRunning = AtomicBoolean(false)

    private val _returnToHome = MutableSharedFlow<Boolean>()
    val returnToHome = _returnToHome.asSharedFlow()

    var stacktrace by mutableStateOf("")
        private set
    val debugInfo: String
        get() = """
            Aliucord Manager ${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} ${if(BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""}

            Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}

            Installing ${installData.discordType} with the ${installData.downloadMethod} apk method

            Failed on: ${currentStep?.name}
        """.trimIndent()

    init {
        viewModelScope.launch(Dispatchers.Main) {
            if (installationRunning.getAndSet(true)) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    when (installData.discordType) {
                        DiscordType.REACT_NATIVE -> installReactNative()
                        DiscordType.KOTLIN -> {}
                    }
                    _returnToHome.emit(true)
                } catch (t: Throwable) {
                    Log.e(
                        BuildConfig.TAG,
                        "$debugInfo\n\n${Log.getStackTraceString(t)}"
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
        val baseApkFile = step(Steps.DL_BASE_APK, StepCategory.APK_DL) {
            externalCacheDir.resolve("base-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    steps[Steps.DL_BASE_APK] = steps[Steps.DL_BASE_APK]!!.copy(cached = true)
                } else downloadManager.downloadDiscordApk(supportedVersion)

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the native libraries split
        val libsApkFile = step(Steps.DL_LIBS_APK, StepCategory.APK_DL) {
            val libArch = arch.replace("-v", "_v")
            externalCacheDir.resolve("config.$libArch-${supportedVersion}.apk").let { file ->
                if (file.exists()){
                    steps[Steps.DL_LIBS_APK] = steps[Steps.DL_LIBS_APK]!!.copy(cached = true)
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.$libArch"
                )

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the locale split
        val localeApkFile = step(Steps.DL_LANG_APK, StepCategory.APK_DL) {
            externalCacheDir.resolve("config.en-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    steps[Steps.DL_LANG_APK] = steps[Steps.DL_LANG_APK]!!.copy(cached = true)
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.en"
                )

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download the drawables split
        val resApkFile = step(Steps.DL_RESC_APK, StepCategory.APK_DL) {
            // TODO: download the appropriate dpi res apk
            externalCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").also { file ->
                if (file.exists()) {
                    steps[Steps.DL_RESC_APK] = steps[Steps.DL_RESC_APK]!!.copy(cached = true)
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.xxhdpi"
                )

                file.copyTo(
                    externalCacheDir
                        .resolve("patched")
                        .resolve(file.name),
                    true
                )
            }
        }

        // Download hermes & cppruntime lib
        val (hermesLibrary, cppRuntimeLibrary) = step(Steps.DL_HERMES, StepCategory.LIB_DL) {
            // Fetch gh releases for Aliucord/Hermes
            val latestHermesRelease = githubRepository.getHermesReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

            // Download the hermes-release.aar file to replace in the apk
            val hermes = externalCacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also

                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-release-${latestHermesRelease.tagName}.aar"
                )
            }

            // Download the hermes-cppruntime-release.aar file to replace in the apk
            val cppruntime = externalCacheDir.resolve("hermes-cppruntime-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also

                downloadManager.download(
                    url = latestHermesRelease.assets.find { it.name == "hermes-cppruntime-release.aar" }!!.browserDownloadUrl,
                    fileName = "hermes-cppruntime-release-${latestHermesRelease.tagName}.aar"
                )
            }

            Pair(hermes, cppruntime)
        }

        // Download Aliucord Native lib
        val aliucordDexFile = step(Steps.DL_ALIUNATIVE, StepCategory.LIB_DL) {
            // Fetch the gh releases for Aliucord/AliucordNative
            val latestAliucordNativeRelease = githubRepository.getAliucordNativeReleases().fold(
                success = { releases ->
                    releases.maxBy { Instant.parse(it.createdAt) }
                },
                fail = { throw it }
            )

            // Download the Aliucord classes.dex file to add to the apk
            externalCacheDir.resolve("classes-${latestAliucordNativeRelease.tagName}.dex").also { file ->
                if (file.exists()) return@also

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
            step(Steps.PATCH_APP_ICON, StepCategory.PATCHING) {
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
        step(Steps.PATCH_MANIFEST, StepCategory.PATCHING) {
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
        step(Steps.PATCH_DEX, StepCategory.PATCHING) {
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
        step(Steps.PATCH_LIBS, StepCategory.PATCHING) {
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

        step(Steps.SIGN_APK, StepCategory.INSTALLING) {
            apks.forEach(Signer::signApk)
        }

        step(Steps.INSTALL_APK, StepCategory.INSTALLING) {
            application.packageManager.packageInstaller
                .installApks(application, *apks)
        }
    }

    @OptIn(ExperimentalTime::class)
    private inline fun <T> step(step: Steps, category: StepCategory, block: () -> T): T {
        steps[step] = steps[step]!!.copy(status = Status.ONGOING)

        currentCategory = category
        currentStep = step

        try {
            val value = measureTimedValue(block)
            val time = value.duration.inWholeMilliseconds.div(1000f)

            steps[step] = steps[step]!!.copy(
                duration = time,
                status = Status.SUCCESSFUL
            )

            currentStep = step
            return value.value
        } catch (t: Throwable) {
            steps[step] = steps[step]!!.copy(status = Status.UNSUCCESSFUL)
            stacktrace = Log.getStackTraceString(t).trim()

            currentCategory = null
            currentStep = step
            throw t
        }
    }

    enum class Steps {
        PATCH_APP_ICON,
        PATCH_MANIFEST,
        PATCH_DEX,
        PATCH_LIBS,
        SIGN_APK,
        INSTALL_APK,

        DL_BASE_APK,
        DL_LIBS_APK,
        DL_LANG_APK,
        DL_RESC_APK,

        DL_HERMES,
        DL_ALIUNATIVE
    }

    enum class StepCategory {
        APK_DL,
        LIB_DL,
        PATCHING,
        INSTALLING
    }

    var currentCategory: StepCategory? by mutableStateOf(StepCategory.APK_DL)
    var currentStep: Steps? by mutableStateOf(null)

    val steps = mutableStateMapOf(
        // Shared
        Steps.PATCH_APP_ICON to Step(
            "Patching app icons",
            Status.QUEUED
        ),

        Steps.PATCH_MANIFEST to Step(
            "Patching apk manifests",
            Status.QUEUED
        ),

        Steps.PATCH_DEX to Step(
            "Adding aliu dex into apk",
            Status.QUEUED
        ),

        Steps.PATCH_LIBS to Step(
            "Replacing libraries",
            Status.QUEUED
        ),

        Steps.SIGN_APK to Step(
            "Signing apks",
            Status.QUEUED
        ),

        Steps.INSTALL_APK to Step(
            "Installing apks",
            Status.QUEUED
        ),

        // React Native
        Steps.DL_BASE_APK to Step(
            "Downloading base apk",
            Status.QUEUED
        ),

        Steps.DL_LIBS_APK to Step(
            "Downloading libraries apk",
            Status.QUEUED
        ),

        Steps.DL_LANG_APK to Step(
            "Downloading locale apk",
            Status.QUEUED
        ),

        Steps.DL_RESC_APK to Step(
            "Downloading resource apk",
            Status.QUEUED
        ),

        Steps.DL_HERMES to Step(
            "Downloading hermes & c++ runtime library",
            Status.QUEUED
        ),

        Steps.DL_ALIUNATIVE to Step(
            "Downloading AliucordNative library",
            Status.QUEUED
        )
    )
}
