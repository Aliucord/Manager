package com.aliucord.manager.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.domain.manager.DownloadManager
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.ui.component.installer.InstallStatus
import com.aliucord.manager.ui.component.installer.InstallStepData
import com.aliucord.manager.ui.dialog.DiscordType
import com.aliucord.manager.ui.screen.InstallData
import com.aliucord.manager.util.*
import com.github.diamondminer88.zip.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
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

    var returnToHome by mutableStateOf(false)

    var isFinished by mutableStateOf(false)
        private set

    var stacktrace by mutableStateOf("")
        private set

    private val debugInfo: String
        get() = """
            Aliucord Manager ${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} ${if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""}

            Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}

            Installing ${installData.discordType} with the ${installData.downloadMethod} apk method

            Failed on: ${currentStep?.name}
        """.trimIndent()

    fun copyDebugToClipboard() {
        val text = "$debugInfo\n\n$stacktrace"
            .replace("(\\\\*~_)".toRegex(), "\\$1")

        application.copyToClipboard(text)
        application.showToast(R.string.action_copied)
    }

    private var debugLogPath by mutableStateOf<String?>(null)
    fun saveDebugToFile() {
        val name = if (debugLogPath != null) {
            debugLogPath!!
        } else {
            "Aliucord Manager ${SimpleDateFormat("YYYY-MM-dd hh-mm-s a").format(Date())}.log"
                .also { debugLogPath = it }
        }

        application.saveFile(name, "$debugInfo\n\n$stacktrace")
    }

    fun clearCache() {
        externalCacheDir.deleteRecursively()
        application.showToast(R.string.action_cleared_cache)
    }

    private val installJob = viewModelScope.launch(Dispatchers.Main) {
        if (installationRunning.getAndSet(true)) {
            return@launch
        }

        withContext(Dispatchers.IO) {
            try {
                when (installData.discordType) {
                    DiscordType.REACT_NATIVE -> installReactNative()
                    DiscordType.KOTLIN -> installKotlin()
                }

                isFinished = true
                delay(20000)
                returnToHome = true
            } catch (t: Throwable) {
                stacktrace = Log.getStackTraceString(t)

                Log.e(
                    BuildConfig.TAG,
                    "$debugInfo\n\n${Log.getStackTraceString(t)}"
                )
            }

            installationRunning.set(false)
        }
    }

    private fun clearOldCache(targetVersion: Int) {
        externalCacheDir.listFiles { f -> f.isDirectory }
            ?.map { it.name.toIntOrNull() to it }
            ?.filter { it.first != null }
            ?.filter { it.first!! in (126021 + 1) until targetVersion }
            ?.forEach { it.second.deleteRecursively() }
    }

    private suspend fun uninstallNewAliucord(targetVersion: Int) {
        val (_, versionCode) = try {
            application.getPackageVersion(preferences.packageName)
        } catch (t: Throwable) {
            return
        }

        if (targetVersion < versionCode) {
            application.uninstallApk(preferences.packageName)

            withContext(Dispatchers.Main) {
                application.showToast(R.string.installer_uninstall_new)
            }

            throw Error("Pleaser uninstall newer Aliucord prior to installing")
        }
    }

    override fun onCleared() {
        if (installationRunning.getAndSet(false)) {
            installJob.cancel("ViewModel cleared")
        }
    }

    private suspend fun installReactNative() {
        steps += listOfNotNull(
            InstallStep.DL_BASE_APK,
            InstallStep.DL_LIBS_APK,
            InstallStep.DL_LANG_APK,
            InstallStep.DL_RESC_APK,
            InstallStep.DL_HERMES,
            InstallStep.DL_ALIUNATIVE,
            if (preferences.replaceIcon) InstallStep.PATCH_APP_ICON else null,
            InstallStep.PATCH_MANIFEST,
            InstallStep.PATCH_DEX,
            InstallStep.PATCH_LIBS,
            InstallStep.SIGN_APK,
            InstallStep.INSTALL_APK,
        ).map {
            it to InstallStepData(it.nameResId, InstallStatus.QUEUED)
        }

        val supportedVersion = preferences.version
        val arch = Build.SUPPORTED_ABIS.first()
        val cacheDir = externalCacheDir
        val discordCacheDir = externalCacheDir.resolve(supportedVersion)
        val patchedDir = discordCacheDir.resolve("patched").also { it.deleteRecursively() }

        clearOldCache(supportedVersion.toInt())
        uninstallNewAliucord(supportedVersion.toInt())

        // Download base.apk
        val baseApkFile = step(InstallStep.DL_BASE_APK) {
            discordCacheDir.resolve("base-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    cached = true
                } else {
                    downloadManager.downloadDiscordApk(supportedVersion, file)
                }

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        // Download the native libraries split
        val libsApkFile = step(InstallStep.DL_LIBS_APK) {
            val libArch = arch.replace("-v", "_v")
            discordCacheDir.resolve("config.$libArch-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    cached = true
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.$libArch",
                    out = file
                )

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        // Download the locale split
        val localeApkFile = step(InstallStep.DL_LANG_APK) {
            discordCacheDir.resolve("config.en-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    cached = true
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.en",
                    out = file
                )

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        // Download the drawables split
        val resApkFile = step(InstallStep.DL_RESC_APK) {
            // TODO: download the appropriate dpi res apk
            discordCacheDir.resolve("config.xxhdpi-${supportedVersion}.apk").let { file ->
                if (file.exists()) {
                    cached = true
                } else downloadManager.downloadSplit(
                    version = supportedVersion,
                    split = "config.xxhdpi",
                    out = file
                )

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        // Download hermes & c++ runtime lib
        val (hermesLibrary, cppRuntimeLibrary) = step(InstallStep.DL_HERMES) {
            // Fetch gh releases for Aliucord/Hermes
            val latestHermesRelease = githubRepository.getHermesRelease().getOrThrow()

            // Download the hermes-release.aar file to replace in the apk
            val hermes = cacheDir.resolve("hermes-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) {
                    cached = true
                    return@also
                }

                latestHermesRelease.assets
                    .find { it.name == "hermes-release.aar" }!!.browserDownloadUrl
                    .also { downloadManager.download(it, file) }
            }

            // Download the hermes-cppruntime-release.aar file to replace in the apk
            val cppRuntime = cacheDir.resolve("hermes-cppruntime-release-${latestHermesRelease.tagName}.aar").also { file ->
                if (file.exists()) return@also
                cached = false

                latestHermesRelease.assets
                    .find { it.name == "hermes-cppruntime-release.aar" }!!.browserDownloadUrl
                    .also { downloadManager.download(it, file) }
            }

            hermes to cppRuntime
        }

        // Download Aliucord Native lib
        val aliucordDexFile = step(InstallStep.DL_ALIUNATIVE) {
            // Fetch the gh releases for Aliucord/AliucordNative
            val latestAliucordNativeRelease = githubRepository.getAliucordNativeRelease().getOrThrow()

            // Download the Aliucord classes.dex file to add to the apk
            cacheDir.resolve("aliucord-${latestAliucordNativeRelease.tagName}.dex").also { file ->
                if (file.exists()) {
                    cached = true
                    return@also
                }

                latestAliucordNativeRelease.assets
                    .find { it.name == "classes.dex" }!!.browserDownloadUrl
                    .also { downloadManager.download(it, file) }
            }
        }

        val apks = arrayOf(baseApkFile, libsApkFile, localeApkFile, resApkFile)

        // Replace app icons
        if (preferences.replaceIcon) {
            step(InstallStep.PATCH_APP_ICON) {
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
        step(InstallStep.PATCH_MANIFEST) {
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
        step(InstallStep.PATCH_DEX) {
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
        step(InstallStep.PATCH_LIBS) {
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

        step(InstallStep.SIGN_APK) {
            apks.forEach(Signer::signApk)
        }

        step(InstallStep.INSTALL_APK) {
            application.installApks(silent = !preferences.devMode, *apks)
        }

        patchedDir.deleteRecursively()
    }

    private suspend fun installKotlin() {
        steps += listOfNotNull(
            InstallStep.FETCH_KT_VERSION,
            InstallStep.DL_KT_APK,
            InstallStep.DL_KOTLIN,
            InstallStep.DL_INJECTOR,
            InstallStep.DL_ALIUHOOK,
            if (preferences.replaceIcon) InstallStep.PATCH_APP_ICON else null,
            InstallStep.PATCH_MANIFEST,
            InstallStep.PATCH_DEX,
            InstallStep.PATCH_LIBS,
            InstallStep.SIGN_APK,
            InstallStep.INSTALL_APK,
        ).map {
            it to InstallStepData(it.nameResId, InstallStatus.QUEUED)
        }

        val dataJson = step(InstallStep.FETCH_KT_VERSION) {
            githubRepository.getDataJson().getOrThrow()
        }

        val arch = Build.SUPPORTED_ABIS.first()
        val cacheDir = externalCacheDir
        val discordCacheDir = externalCacheDir.resolve(dataJson.versionCode)
        val patchedDir = discordCacheDir.resolve("patched").also { it.deleteRecursively() }

        dataJson.versionCode.toInt().also {
            clearOldCache(it)
            uninstallNewAliucord(it)
        }

        // Download base.apk
        val baseApkFile = step(InstallStep.DL_KT_APK) {
            discordCacheDir.resolve("base.apk").let { file ->
                if (file.exists()) {
                    cached = true
                } else {
                    downloadManager.downloadDiscordApk(dataJson.versionCode, file)
                }

                file.copyTo(
                    patchedDir.resolve(file.name),
                    true
                )
            }
        }

        val kotlinFile = step(InstallStep.DL_KOTLIN) {
            cacheDir.resolve("kotlin.dex").also { file ->
                if (file.exists()) {
                    cached = true
                } else {
                    downloadManager.downloadKotlinDex(file)
                }
            }
        }


        // Download the injector dex
        val injectorFile = step(InstallStep.DL_INJECTOR) {
            cacheDir.resolve("injector-${dataJson.aliucordHash}.dex").also { file ->
                if (file.exists()) {
                    cached = true
                } else {
                    downloadManager.downloadKtInjector(file)
                }
            }
        }

        // Download Aliuhook aar
        val aliuhookAarFile = step(InstallStep.DL_ALIUHOOK) {
            // Fetch aliuhook version
            val aliuhookVersion = aliucordMaven.getAliuhookVersion().getOrThrow()

            // Download aliuhook aar
            cacheDir.resolve("aliuhook-${aliuhookVersion}.aar").also { file ->
                if (file.exists()) {
                    cached = true
                } else {
                    downloadManager.downloadAliuhook(aliuhookVersion, file)
                }
            }
        }

        // Replace app icons
        if (preferences.replaceIcon) {
            step(InstallStep.PATCH_APP_ICON) {
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
        step(InstallStep.PATCH_MANIFEST) {
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
        val dexCount = step(InstallStep.PATCH_DEX) {
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
            }

            dexCount
        }

        // Replace libs
        step(InstallStep.PATCH_LIBS) {
            ZipWriter(baseApkFile, true).use { baseApk ->
                ZipReader(aliuhookAarFile).use { aliuhookAar ->
                    for (libFile in arrayOf("libaliuhook.so", "libc++_shared.so", "liblsplant.so")) {
                        val bytes = aliuhookAar.openEntry("jni/$arch/$libFile")?.read()
                            ?: throw IllegalStateException("Failed to read $libFile from aliuhook aar")

                        baseApk.writeEntry("lib/$arch/$libFile", bytes)
                    }

                    // Add Aliuhook's dex file
                    val aliuhookDex = aliuhookAar.openEntry("classes.dex")?.read()
                        ?: throw IllegalStateException("No classes.dex in aliuhook aar")

                    baseApk.writeEntry("classes${dexCount + 3}.dex", aliuhookDex)
                }
            }
        }

        step(InstallStep.SIGN_APK) {
            Signer.signApk(baseApkFile)
        }

        step(InstallStep.INSTALL_APK) {
            application.installApks(silent = !preferences.devMode, baseApkFile)
        }

        patchedDir.deleteRecursively()
    }

    @OptIn(ExperimentalTime::class)
    private inline fun <T> step(step: InstallStep, block: InstallStepData.() -> T): T {
        steps[step]!!.status = InstallStatus.ONGOING
        currentStep = step

        try {
            val value = measureTimedValue { block.invoke(steps[step]!!) }
            val millis = value.duration.inWholeMilliseconds

            // Add delay for human psychology + groups are switched too fast
            if (!preferences.devMode && millis < 1000) {
                Thread.sleep(1000 - millis)
            }

            steps[step]!!.apply {
                duration = millis.div(1000f)
                status = InstallStatus.SUCCESSFUL
            }

            currentStep = step
            return value.value
        } catch (t: Throwable) {
            steps[step]!!.status = InstallStatus.UNSUCCESSFUL

            currentStep = step
            throw t
        }
    }

    enum class InstallStepGroup(
        @StringRes
        val nameResId: Int
    ) {
        APK_DL(R.string.install_group_apk_dl),
        LIB_DL(R.string.install_group_lib_dl),
        PATCHING(R.string.install_group_patch),
        INSTALLING(R.string.install_group_install)
    }

    // Order matters, define it in the same order as it is patched
    enum class InstallStep(
        val group: InstallStepGroup,

        @StringRes
        val nameResId: Int
    ) {
        // React Native
        DL_BASE_APK(InstallStepGroup.APK_DL, R.string.install_step_dl_apk_base),
        DL_LIBS_APK(InstallStepGroup.APK_DL, R.string.install_step_dl_apk_lib),
        DL_LANG_APK(InstallStepGroup.APK_DL, R.string.install_step_dl_apk_locale),
        DL_RESC_APK(InstallStepGroup.APK_DL, R.string.install_step_dl_apk_resource),

        DL_HERMES(InstallStepGroup.LIB_DL, R.string.install_step_dl_lib_hermes),
        DL_ALIUNATIVE(InstallStepGroup.LIB_DL, R.string.install_step_dl_lib_aliunative),

        // Kotlin
        FETCH_KT_VERSION(InstallStepGroup.APK_DL, R.string.install_step_fetch_kt_version),
        DL_KT_APK(InstallStepGroup.APK_DL, R.string.install_step_dl_kt_apk),
        DL_KOTLIN(InstallStepGroup.LIB_DL, R.string.install_step_dl_kotlin),
        DL_INJECTOR(InstallStepGroup.LIB_DL, R.string.install_step_dl_injector),
        DL_ALIUHOOK(InstallStepGroup.LIB_DL, R.string.install_step_dl_aliuhook),

        // Common
        PATCH_APP_ICON(InstallStepGroup.PATCHING, R.string.install_step_patch_icons),
        PATCH_MANIFEST(InstallStepGroup.PATCHING, R.string.install_step_patch_manifests),
        PATCH_DEX(InstallStepGroup.PATCHING, R.string.install_step_patch_dex),
        PATCH_LIBS(InstallStepGroup.PATCHING, R.string.install_step_patch_libs),
        SIGN_APK(InstallStepGroup.INSTALLING, R.string.install_step_signing),
        INSTALL_APK(InstallStepGroup.INSTALLING, R.string.install_step_installing);
    }

    var currentStep: InstallStep? by mutableStateOf(null)
    val steps = mutableStateMapOf<InstallStep, InstallStepData>()

    // TODO: cache this instead
    fun getSteps(group: InstallStepGroup): List<InstallStepData> {
        return steps
            .filterKeys { it.group == group }.entries
            .sortedBy { it.key.ordinal }
            .map { it.value }
    }
}
