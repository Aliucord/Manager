package com.aliucord.manager.patcher.steps.download

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.OverlayManager
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.aliucord.manager.ui.components.dialogs.CustomComponentVersionPicker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a zip of all the smali patches to be applied to the APK during patching.
 */
@Stable
class DownloadPatchesStep : DownloadStep(), KoinComponent {
    private val paths: PathManager by inject()
    private val overlays: OverlayManager by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    private var isCustomVersion: Boolean = false
    lateinit var targetVersion: SemVer
        private set

    override val localizedName = R.string.patch_step_dl_smali
    override val targetUrl get() = URL
    override val targetFile get() = paths.cachedSmaliPatches(targetVersion, isCustomVersion)

    override suspend fun execute(container: StepRunner) {
        val customVersions = mutableStateListOf<SemVer>()
            .apply { addAll(paths.customSmaliPatches()) }

        // Prompt to select or manage custom versions instead of downloading
        if (customVersions.isNotEmpty()) {
            container.log("Custom versions present, waiting for selection: ${customVersions.joinToString()}")
            val selectedVersion = overlays.startComposableForResult { callback ->
                CustomComponentVersionPicker(
                    componentTitle = "Smali Patches",
                    versions = customVersions,
                    onConfirm = { version -> callback(version) },
                    onDelete = { version ->
                        try {
                            paths.cachedSmaliPatches(version, custom = true).delete()
                            customVersions.remove(version)

                            // Dismiss if no custom versions left
                            if (customVersions.isEmpty())
                                callback(null)
                        } catch (t: Throwable) {
                            Log.e(BuildConfig.TAG, "Failed to delete custom component", t)
                        }
                    },
                    onCancel = { callback(null) },
                )
            }

            if (selectedVersion != null) {
                isCustomVersion = true
                targetVersion = selectedVersion
                state = StepState.Skipped
                container.log("Using local custom patches version $selectedVersion")
                return
            } else {
                container.log("Selection dialog cancelled, continuing as normal")
            }
        }

        targetVersion = container.getStep<FetchInfoStep>()
            .data.patchesVersion
        container.log("Downloading patches version $targetVersion")

        super.execute(container)
    }

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/patches.zip"
    }
}
