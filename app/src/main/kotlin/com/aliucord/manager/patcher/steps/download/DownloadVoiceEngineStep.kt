package com.aliucord.manager.patcher.steps.download

import android.os.Build
import androidx.compose.runtime.Stable
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.patch.PatchVoiceEngineStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download the Discord split APK matching the device's primary ABI, which carries the
 * libdiscord.so used as the voice engine (consumed by [PatchVoiceEngineStep]).
 */
@Stable
class DownloadVoiceEngineStep : DownloadStep<Int>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_voice_engine

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().data.voiceEngineVersionCode

    override fun getRemoteUrl(container: StepRunner) = getDiscordSplitApkUrl(
        version = getVersion(container),
        split = "split_config.$currentDeviceArch".replace('-', '_'),
    )

    override fun getStoredFile(container: StepRunner) =
        paths.cachedDiscordApk(getVersion(container), split = "split_config.$currentDeviceArch")

    private companion object {
        val currentDeviceArch: String = Build.SUPPORTED_ABIS.first()

        fun getDiscordSplitApkUrl(version: Int, split: String): String =
            "${BuildConfig.MAVEN_URL}/com/discord/discord/$version/$split.apk"
    }
}
