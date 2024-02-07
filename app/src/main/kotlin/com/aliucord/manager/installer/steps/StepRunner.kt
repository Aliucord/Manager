package com.aliucord.manager.installer.steps

import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.manager.PreferencesManager
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

abstract class StepRunner : KoinComponent {
    private val preferences: PreferencesManager by inject()

    abstract val steps: ImmutableList<Step>

    /**
     * Get a step that has already been successfully executed.
     * This is used to retrieve previously executed dependency steps from a later step.
     */
    inline fun <reified T : Step> getStep(): T {
        val step = steps.asSequence()
            .filterIsInstance<T>()
            .filter { it.state.isFinished }
            .firstOrNull()

        if (step == null) {
            throw IllegalArgumentException("No completed step ${T::class.simpleName} exists in container")
        }

        return step
    }

    suspend fun executeAll(): Throwable? {
        for (step in steps) {
            val error = step.executeCatching(this@StepRunner)
            if (error != null) return error

            // Skip minimum run time when in dev mode
            if (!preferences.devMode && step.durationMs < MINIMUM_STEP_DELAY) {
                delay(MINIMUM_STEP_DELAY - step.durationMs)
            }
        }

        return null
    }
}
