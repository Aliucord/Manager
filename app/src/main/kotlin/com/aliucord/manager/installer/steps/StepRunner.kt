package com.aliucord.manager.installer.steps

import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.manager.PreferencesManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
            .filter { it.state == StepState.Success }
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

            // Add delay for human psychology and
            // better group visibility in UI (the active group can change way too fast)
            if (!preferences.devMode && step.durationMs < 1000) {
                delay(1000L - step.durationMs)
            }
        }

        return null
    }
}
