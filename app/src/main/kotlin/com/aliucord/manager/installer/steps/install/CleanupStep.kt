package com.aliucord.manager.installer.steps.install

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.manager.PreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Cleanup patching working directory once the installation has completed.
 */
class CleanupStep : Step(), KoinComponent {
    private val paths: PathManager by inject()
    private val prefs: PreferencesManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.install_step_cleanup

    override suspend fun execute(container: StepContainer) {
        if (prefs.keepPatchedApks) {
            state = StepState.Skipped
        } else {
            if (!paths.patchingWorkingDir().deleteRecursively())
                throw IllegalStateException("Failed to delete patching working dir")
        }
    }
}
