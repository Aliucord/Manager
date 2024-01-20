package com.aliucord.manager.installer.steps.prepare

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.network.dto.Version
import com.aliucord.manager.network.service.AliucordGithubService
import com.aliucord.manager.network.utils.getOrThrow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Stable
class FetchInfoStep : Step(), KoinComponent {
    private val github: AliucordGithubService by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.install_step_fetch_kt_version

    /**
     * Fetched data about the latest Aliucord commit and supported Discord version.
     */
    lateinit var data: Version

    override suspend fun execute(container: StepContainer) {
        data = github.getDataJson().getOrThrow()
    }
}
