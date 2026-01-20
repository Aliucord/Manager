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
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.github.diamondminer88.zip.ZipReader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest Aliuhook build from the Aliucord maven.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadAliuhookStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val maven: AliucordMavenService by inject()

    override val localizedName = R.string.patch_step_dl_aliuhook

    override fun getRemoteUrl(container: StepRunner) =
        maven.getAliuhookUrl(getVersion(container))

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().aliuhookVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedAliuhookAAR(getVersion(container))

    override val dexPriority = 0
    override val dexCount = 1
    override fun getDexFiles(container: StepRunner): List<ByteArray> {
        val dexBytes = ZipReader(getStoredFile(container)).use { zip ->
            zip.openEntry("classes.dex")?.read()
                ?: throw IllegalStateException("No prebuilt classes.dex in downloaded aliuhook build")
        }

        return listOf(dexBytes)
    }
}
