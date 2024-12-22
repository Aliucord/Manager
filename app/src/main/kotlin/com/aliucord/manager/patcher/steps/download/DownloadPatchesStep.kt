package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.OverlayManager
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
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
    lateinit var targetVersion: SemVer
        private set

    override val localizedName = R.string.patch_step_dl_smali
    override val targetUrl get() = URL
    override val targetFile get() = paths.cachedSmaliPatches(targetVersion)

    override suspend fun execute(container: StepRunner) {
        targetVersion = container.getStep<FetchInfoStep>()
            .data.patchesVersion

        super.execute(container)
    }

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/patches.zip"
    }
}
