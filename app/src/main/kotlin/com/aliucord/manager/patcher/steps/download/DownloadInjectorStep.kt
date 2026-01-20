package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadInjectorStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_injector

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().data.injectorVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedInjectorDex(getVersion(container))

    override val dexCount = 1
    override val dexPriority = 3
    override fun getDexFiles(container: StepRunner) =
        listOf(getStoredFile(container).readBytes())

    private companion object {
        const val URL = "https://builds.aliucord.com/Injector.dex"
    }
}
