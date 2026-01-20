package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
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
class DownloadPatchesStep : DownloadStep<SemVer>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_smali
    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().data.patchesVersion

    override fun getRemoteUrl(container: StepRunner) = URL
    override fun getStoredFile(container: StepRunner) =
        paths.cachedSmaliPatches(getVersion(container))

    private companion object {
        const val URL = "https://builds.aliucord.com/patches.zip"
    }
}
