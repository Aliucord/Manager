package com.aliucord.manager.installer.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.base.DownloadStep
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.getOrThrow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest Aliuhook build from the Aliucord maven.
 */
@Stable
class DownloadAliuhookStep : DownloadStep(), KoinComponent {
    private val paths: PathManager by inject()
    private val maven: AliucordMavenRepository by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    private lateinit var targetVersion: String

    override val localizedName = R.string.install_step_dl_aliuhook
    override val targetUrl get() = AliucordMavenRepository.getAliuhookUrl(targetVersion)
    override val targetFile get() = paths.cachedAliuhookAAR(targetVersion)

    override suspend fun execute(container: StepContainer) {
        targetVersion = maven.getAliuhookVersion().getOrThrow()

        super.execute(container)
    }
}

