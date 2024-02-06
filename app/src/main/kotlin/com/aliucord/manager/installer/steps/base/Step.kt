package com.aliucord.manager.installer.steps.base

import androidx.annotation.StringRes
import androidx.compose.runtime.*
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import org.koin.core.time.measureTimedValue
import kotlin.math.roundToInt

/**
 * A base install process step. Steps are single-use
 */
@Stable
abstract class Step {
    /**
     * The group this step belongs to.
     */
    abstract val group: StepGroup

    /**
     * The UI name to display this step as
     */
    @get:StringRes
    abstract val localizedName: Int

    /**
     * Run the step's logic.
     * It can be assumed that this is executed in the correct order after other steps.
     */
    protected abstract suspend fun execute(container: StepRunner)

    /**
     * The current state of this step in the installation process.
     */
    var state by mutableStateOf(StepState.Pending)
        protected set

    /**
     * If the current state is [StepState.Running], then the progress of this step.
     * If the progress isn't currently measurable, then this should be set to `-1`.
     */
    var progress by mutableFloatStateOf(-1f)
        protected set

    /**
     * The total execution time once this step has finished execution.
     */
    // TODO: make this a live value
    var durationMs by mutableIntStateOf(0)
        private set

    /**
     * Thin wrapper over [execute] but handling errors.
     * @return An exception if the step failed to execute.
     */
    suspend fun executeCatching(container: StepRunner): Throwable? {
        if (state != StepState.Pending)
            throw IllegalStateException("Cannot execute a step that has already started")

        state = StepState.Running

        // Execute this steps logic while timing it
        val (error, executionTimeMs) = measureTimedValue {
            try {
                execute(container)

                if (state != StepState.Skipped)
                    state = StepState.Success

                null
            } catch (t: Throwable) {
                state = StepState.Error
                t
            }
        }

        durationMs = executionTimeMs.roundToInt()
        return error
    }
}
