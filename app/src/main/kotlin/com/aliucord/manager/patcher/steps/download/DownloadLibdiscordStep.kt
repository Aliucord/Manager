package com.aliucord.manager.patcher.steps.download

import android.os.Build
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.services.AliucordMavenService
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.patch.ReplaceLibdiscordStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download the Discord split APK matching the device's primary ABI, which carries the
 * libdiscord.so used as the voice engine (consumed by [ReplaceLibdiscordStep]).
 */
@Stable
class DownloadLibdiscordStep : DownloadStep<Int>(), KoinComponent {
    private val paths: PathManager by inject()
    private val maven: AliucordMavenService by inject()

    val currentDeviceArch: String = Build.SUPPORTED_ABIS.first()

    override val localizedName = R.string.patch_step_dl_libdiscord

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().libdiscordVersion

    override fun getRemoteUrl(container: StepRunner) =
        maven.getLibraryApkUrl(getVersion(container), currentDeviceArch)

    override fun getStoredFile(container: StepRunner) =
        paths.cachedLibdiscordApk(getVersion(container), currentDeviceArch)
}
