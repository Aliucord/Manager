package com.aliucord.manager.patcher.steps.prepare

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.services.AliucordMavenService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fetches versions data from various sources to be used during the installation.
 */
@Stable
class FetchInfoStep : Step(), KoinComponent {
    private val github: AliucordGithubService by inject()
    private val maven: AliucordMavenService by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_fetch_kt_version

    /**
     * Remote build data about the latest versions of components.
     */
    lateinit var data: BuildInfo
        private set

    /**
     * Remote data about the latest Aliuhook version available from the Aliucord maven.
     */
    lateinit var aliuhookVersion: SemVer
        private set

    override suspend fun execute(container: StepRunner) {
        container.log("Fetching ${AliucordGithubService.DATA_JSON_URL}")
        data = github.getBuildData(force = true).getOrThrow()
        container.log("Fetched build data: $data")

        container.log("Obtaining latest aliuhook version")
        aliuhookVersion = maven.getAliuhookVersion(force = true).getOrThrow()
        container.log("Fetched aliuhook version: $aliuhookVersion")
    }
}
