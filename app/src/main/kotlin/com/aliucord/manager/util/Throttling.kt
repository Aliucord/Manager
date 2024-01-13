package com.aliucord.manager.util

import kotlinx.coroutines.*

/**
 * Constructs a function that executes [destinationFunction] only once per [skipMs].
 */
inline fun throttle(
    skipMs: Long,
    coroutineScope: CoroutineScope,
    crossinline destinationFunction: suspend () -> Unit
): () -> Unit {
    var throttleJob: Job? = null
    return {
        if (throttleJob?.isCompleted != false) {
            throttleJob = coroutineScope.launch {
                destinationFunction()
                delay(skipMs)
            }
        }
    }
}
