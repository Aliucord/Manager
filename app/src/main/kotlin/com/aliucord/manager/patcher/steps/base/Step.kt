package com.aliucord.manager.patcher.steps.base

import androidx.annotation.StringRes
import androidx.compose.runtime.*
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.util.toPrecision
import kotlinx.coroutines.*
import kotlin.math.roundToLong
import kotlin.time.measureTimedValue

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

    private val durationSecs = mutableFloatStateOf(0f)
    private var startTime: Long? = null
    private var totalTimeMs: Long? = null

    /**
     * The total amount of time this step has/was executed for in milliseconds.
     * If this step has not started executing then it will return `0`.
     */
    fun getDuration(): Long {
        // Step hasn't started executing
        val startTime = startTime ?: return 0

        // Step already finished executing
        totalTimeMs?.let { return it }

        return System.currentTimeMillis() - startTime
    }

    /**
     * The live execution time of this step in seconds.
     * The value is clamped to a resolution of 10ms updated every 50ms.
     */
    @Composable
    fun collectDurationAsState(): State<Float> {
        if (state.isFinished)
            return durationSecs

        LaunchedEffect(state) {
            while (true) {
                durationSecs.floatValue = (getDuration() / 1000.0)
                    .toPrecision(2).toFloat()

                delay(50)
            }
        }

        return durationSecs
    }

    /**
     * Thin wrapper over [execute] but handling errors.
     * @return An exception if the step failed to execute.
     */
    suspend fun executeCatching(container: StepRunner): Throwable? {
        if (state != StepState.Pending)
            throw IllegalStateException("Cannot execute a step that has already started")

        state = StepState.Running
        startTime = System.currentTimeMillis()

        // Execute this steps logic while timing it
        val (error, executionTime) = measureTimedValue {
            try {
                withContext(Dispatchers.Default) {
                    execute(container)
                }

                if (state != StepState.Skipped)
                    state = StepState.Success

                null
            } catch (t: Throwable) {
                state = StepState.Error
                t
            }
        }

        totalTimeMs = executionTime.inWholeMilliseconds
        durationSecs.floatValue = executionTime.inWholeMilliseconds / 1000f

        return error
    }
}
