package com.aliucord.manager.installer

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.DownloadStep
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.util.InstallNotifications
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The minimum time that is required to occur between step switches, to avoid
 * quickly switching the step groups in the UI. (very disorienting)
 * Larger delay leads to a perception that it's doing more work than it actually is.
 */
const val MINIMUM_STEP_DELAY: Long = 600L

const val ERROR_NOTIF_ID = 200002

abstract class StepRunner : KoinComponent {
    private val context: Context by inject()
    private val preferences: PreferencesManager by inject()

    abstract val steps: ImmutableList<Step>

    /**
     * Get a step that has already been successfully executed.
     * This is used to retrieve previously executed dependency steps from a later step.
     * @param completed Only match steps that have finished executing.
     */
    inline fun <reified T : Step> getStep(completed: Boolean = true): T {
        val step = steps.asSequence()
            .filterIsInstance<T>()
            .filter { !completed || it.state.isFinished }
            .firstOrNull()

        if (step == null) {
            throw IllegalArgumentException("No completed step ${T::class.simpleName} exists in container")
        }

        return step
    }

    suspend fun executeAll(): Throwable? {
        for (step in steps) {
            val error = step.executeCatching(this@StepRunner)
            if (error != null) {
                showErrorNotification()

                // If this is a patch step and it failed, then clear download cache just in case
                if (step.group == StepGroup.Patch && !preferences.devMode) {
                    for (downloadStep in steps.asSequence().filterIsInstance<DownloadStep>()) {
                        downloadStep.targetFile.delete()
                    }
                }

                return error
            }

            // Skip minimum run time when in dev mode
            val duration = step.getDuration()
            if (!preferences.devMode && duration < MINIMUM_STEP_DELAY) {
                delay(MINIMUM_STEP_DELAY - duration)
            }
        }

        return null
    }

    private fun showErrorNotification() {
        // If app backgrounded
        if (ProcessLifecycleOwner.get().lifecycle.currentState == Lifecycle.State.CREATED) {
            InstallNotifications.createNotification(
                context = context,
                id = ERROR_NOTIF_ID,
                title = R.string.notif_install_fail_title,
                description = R.string.notif_install_fail_desc,
            )
        }
    }
}
