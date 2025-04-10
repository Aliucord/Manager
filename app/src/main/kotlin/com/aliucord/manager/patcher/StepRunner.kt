package com.aliucord.manager.patcher

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.Step
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
private const val MINIMUM_STEP_DELAY: Long = 600L

/**
 * ID used for showing error notifications emanating from this step runner.
 */
private const val ERROR_NOTIF_ID = 200002

abstract class StepRunner : KoinComponent {
    private val context: Context by inject()
    private val preferences: PreferencesManager by inject()

    private val logEntries: MutableList<String> = mutableListOf()

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

    /**
     * Adds a log entry without any associated log level.
     */
    fun log(text: String) {
        logEntries += text
        Log.i(BuildConfig.TAG, text)
    }

    /**
     * Combines all the log entries into a single formatted log.
     */
    fun getLog(): String = logEntries.joinToString(separator = "\n")

    suspend fun executeAll(): Throwable? {
        log("Starting step runner")
        log("Registered steps: " + steps.joinToString { it.javaClass.simpleName })

        for (step in steps) {
            val stepName = step.javaClass.simpleName

            log("Running step: $stepName")
            val error = step.executeCatching(this@StepRunner)

            if (error != null) {
                log("Failed on step: $stepName after ${step.getDuration()}ms")
                showErrorNotification()

                // If this is a patch step and it failed, then clear download cache just in case
                if (step.group == StepGroup.Patch && !preferences.devMode) {
                    log("Deleting download cache")
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

            log("Completed step: $stepName in ${duration}ms")
        }

        log("Successfully finished all steps in ${steps.sumOf { it.getDuration() }}ms")

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
