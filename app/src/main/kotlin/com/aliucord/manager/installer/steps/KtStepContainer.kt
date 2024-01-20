package com.aliucord.manager.installer.steps

import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.*
import com.aliucord.manager.installer.steps.prepare.FetchInfoStep
import kotlinx.collections.immutable.persistentListOf

/**
 * Used for installing the old Kotlin Discord app.
 */
class KtStepContainer : StepContainer() {
    override val steps = persistentListOf<Step>(
        FetchInfoStep(),
        DownloadDiscordStep(),
        DownloadInjectorStep(),
        DownloadAliuhookStep(),
        DownloadKotlinStep(),
    )
}
