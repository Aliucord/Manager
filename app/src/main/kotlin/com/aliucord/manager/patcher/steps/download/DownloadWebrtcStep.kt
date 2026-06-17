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
 * Download the most recent available prebuilt WebRTC fork build.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadWebrtcStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_webrtc

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().data.webrtcVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedWebrtcDex(getVersion(container))

    override val dexCount = 1
    override val dexPriority = -1
    override fun getDexFiles(container: StepRunner) =
        listOf(getStoredFile(container).readBytes())

    private companion object {
        const val URL = "https://builds.aliucord.com/webrtc.dex"
    }
}
