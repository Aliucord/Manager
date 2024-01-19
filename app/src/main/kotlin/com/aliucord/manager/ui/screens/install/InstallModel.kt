package com.aliucord.manager.ui.screens.install

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.DownloadManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.ui.components.installer.InstallStatus
import com.aliucord.manager.ui.components.installer.InstallStepData
import com.aliucord.manager.util.*
import com.github.diamondminer88.zip.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.measureTimedValue

class InstallModel(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesManager,
    private val githubRepository: GithubRepository,
    private val aliucordMaven: AliucordMavenRepository,
    private val installData: InstallData,
) : ScreenModel {
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

            Installing Aliucord kt with the ${installData.downloadMethod} apk method

            Failed on: ${currentStep?.name}
        """.trimIndent()

    fun copyDebugToClipboard() {
        val text = "$debugInfo\n\n$stacktrace"
            .replace("(\\\\*~_)".toRegex(), "\\$1")

        application.copyToClipboard(text)
        application.showToast(R.string.action_copied)
    }

    private var debugLogPath by mutableStateOf<String?>(null)

    @SuppressLint("SimpleDateFormat")
    fun saveDebugToFile() {
        val name = if (debugLogPath != null) {
            debugLogPath!!
        } else {
            "Aliucord Manager ${SimpleDateFormat("yyyy-MM-dd hh-mm-s a").format(Date())}.log"
                .also { debugLogPath = it }
        }

        application.saveFile(name, "$debugInfo\n\n$stacktrace")
    }

    fun clearCache() {
        externalCacheDir.deleteRecursively()
        application.showToast(R.string.action_cleared_cache)
    }

    private val installJob = screenModelScope.launch(Dispatchers.Main) {
        if (installationRunning.getAndSet(true)) {
            return@launch
        }

        withContext(Dispatchers.IO) {
            try {
                installKotlin()

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

    override fun onDispose() {
        if (installationRunning.getAndSet(false)) {
            installJob.cancel("ViewModel cleared")
        }
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
            // Align resources.arsc due to targeting api 30 for silent install
            if (Build.VERSION.SDK_INT >= 31) {
                val bytes = ZipReader(baseApkFile).use {
                    if (it.entryNames.contains("resources.arsc")) {
                        it.openEntry("resources.arsc")?.read()
                    } else {
                        null
                    }
                }

                ZipWriter(baseApkFile, true).use {
                    it.deleteEntry("resources.arsc", true)
                    it.writeEntry("resources.arsc", bytes, ZipCompression.NONE, 4096)
                }
            }

            Signer.signApk(baseApkFile)
        }

        step(InstallStep.INSTALL_APK) {
            application.installApks(silent = !preferences.devMode, baseApkFile)

            if (!preferences.keepPatchedApks) {
                patchedDir.deleteRecursively()
            }
        }
    }

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
        val nameResId: Int,
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
        val nameResId: Int,
    ) {
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
