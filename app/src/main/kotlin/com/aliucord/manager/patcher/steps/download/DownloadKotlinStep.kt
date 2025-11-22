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
 * Download the most recent available Kotlin stdlib build that is supported.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadKotlinStep : DownloadStep(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    lateinit var targetVersion: SemVer
        private set

    override val localizedName = R.string.patch_step_dl_kotlin
    override val targetUrl = URL
    override val targetFile get() = paths.cachedKotlinDex(targetVersion)

    override suspend fun execute(container: StepRunner) {
        targetVersion = container.getStep<FetchInfoStep>()
            .data.injectorVersion
        container.log("Downloading Kotlin stdlib version $targetVersion")

        super.execute(container)
    }

    private companion object {
        const val URL = "https://builds.aliucord.com/kotlin.dex"
    }

    override val dexCount = 1
    override val dexPriority = -1
    override fun getDexFiles() = listOf(targetFile.readBytes())
}
