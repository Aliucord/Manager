package com.aliucord.manager.patcher.steps.install

import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Cleanup patching working directory once the installation has completed.
 */
class CleanupStep : Step(), KoinComponent {
    private val paths: PathManager by inject()
    private val prefs: PreferencesManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_cleanup

    override suspend fun execute(container: StepRunner) {
        if (prefs.keepPatchedApks) {
            state = StepState.Skipped
        } else {
            if (!paths.patchingWorkingDir().deleteRecursively())
                throw IllegalStateException("Failed to delete patching working dir")
        }
    }
}
