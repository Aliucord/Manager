package com.aliucord.manager.patcher.steps.prepare

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Stable
class FetchInfoStep : Step(), KoinComponent {
    private val github: AliucordGithubService by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_fetch_kt_version

    /**
     * Fetched data about the latest Aliucord commit and supported Discord version.
     */
    lateinit var data: BuildInfo
        private set

    override suspend fun execute(container: StepRunner) {
        container.log("Fetching ${AliucordGithubService.DATA_JSON_URL}")
        data = github.getDataJson().getOrThrow()
        container.log("Fetched build data: $data")
    }
}
