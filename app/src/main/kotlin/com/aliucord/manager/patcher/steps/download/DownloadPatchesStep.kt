package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.aliucord.manager.ui.screens.patchopts.PatchCustomComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a zip of all the smali patches to be applied to the APK during patching.
 */
@Stable
class DownloadPatchesStep(
    private val custom: PatchCustomComponent?,
) : DownloadStep<SemVer>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_smali

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        custom?.version ?: container.getStep<FetchInfoStep>().data.patchesVersion

    override fun getStoredFile(container: StepRunner) =
        custom?.file ?: paths.cachedSmaliPatches(getVersion(container))

    override suspend fun execute(container: StepRunner) {
        if (custom != null) {
            container.log("Using custom patches with version ${custom.version} built ${custom.buildTime}")
        }

        super.execute(container)
    }

    private companion object {
        const val URL = "https://builds.aliucord.com/patches.zip"
    }
}
