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

    // R8/d8 may split across webrtc.dex + classes*.dex
    private var dexFiles: List<ByteArray>? = null

    override suspend fun execute(container: StepRunner) {
        super.execute(container)

        dexFiles = ZipReader(getStoredFile(container)).use { aar ->
            aar.entryNames
                .filter { !it.contains('/') && it.endsWith(".dex") }
                .sorted()
                .map { name ->
                    aar.openEntry(name)?.read() ?: throw IllegalStateException("Failed to read $name from aliuvoice aar")
                }
        }

        if (dexFiles!!.isEmpty())
            throw IllegalStateException("No prebuilt dex files in downloaded aliuvoice build")
    }

    override fun getDexFiles(container: StepRunner): List<ByteArray> =
        dexFiles ?: throw IllegalStateException("Aliuvoice dex files not loaded, download step likely failed")
}
