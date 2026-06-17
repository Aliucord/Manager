package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.services.AliucordMavenService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.aliucord.manager.patcher.steps.patch.ReplaceLibdiscordStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.github.diamondminer88.zip.ZipReader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest Aliuvoice build from the Aliucord maven.
 *  * The AAR bundles the Sunflower libdiscord.so libs (consumed by [ReplaceLibdiscordStep])
 *  * and a prebuilt webrtc.dex.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadAliuvoiceStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val maven: AliucordMavenService by inject()

    override val localizedName = R.string.patch_step_dl_aliuvoice

    override fun getRemoteUrl(container: StepRunner) =
        maven.getAliuvoiceUrl(getVersion(container))

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().aliuvoiceVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedAliuvoiceAAR(getVersion(container))

    override val dexPriority = 0
    override val dexCount = 1
    override fun getDexFiles(container: StepRunner): List<ByteArray> {
        val dexBytes = ZipReader(getStoredFile(container)).use { zip ->
            zip.openEntry("webrtc.dex")?.read()
                ?: throw IllegalStateException("No prebuilt webrtc.dex in downloaded aliuvoice build")
        }

        return listOf(dexBytes)
    }
}
