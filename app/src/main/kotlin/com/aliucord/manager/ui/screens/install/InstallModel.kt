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
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.manager.*
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
    private val paths: PathManager,
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesManager,
    private val githubRepository: GithubRepository,
    private val aliucordMaven: AliucordMavenRepository,
) : ScreenModel {
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
        paths.clearCache()
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

    private suspend fun installKotlin() {
        steps += listOfNotNull(
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

    // Order matters, define it in the same order as it is patched
    enum class InstallStep(
        @StringRes
        val nameResId: Int,
    ) {
        // Common
        PATCH_APP_ICON(InstallStepGroup.PATCHING, R.string.install_step_patch_icons),
        PATCH_MANIFEST(InstallStepGroup.PATCHING, R.string.install_step_patch_manifests),
        PATCH_DEX(InstallStepGroup.PATCHING, R.string.install_step_patch_dex),
        PATCH_LIBS(InstallStepGroup.PATCHING, R.string.install_step_patch_libs),
        SIGN_APK(InstallStepGroup.INSTALLING, R.string.install_step_signing),
        INSTALL_APK(InstallStepGroup.INSTALLING, R.string.install_step_installing);
    }
}
