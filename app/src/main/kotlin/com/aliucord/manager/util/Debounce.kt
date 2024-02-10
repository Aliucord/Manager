package com.aliucord.manager.util

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Returns a function that when called any number of times, only executes [function] after
 * [waitMs] has passed after the last call, using the last call's passed in param(s).
 * This is for things like "throttling" search text input to reduce the amount of actual
 * calls to only when the user stops typing for example.
 *
 * @param waitMs Milliseconds to wait for additional calls before executing [function]
 * @param waitCompletion If true, then ignore any calls while [function] is executing instead of
 * 						 cancelling it and restarting the debouncing job.
 * @param function The target function to debounce.
 * @return Refer to the general description
 */
fun <P1, P2> CoroutineScope.debounce(
    waitMs: Long,
    waitCompletion: Boolean = false,
    function: suspend (P1, P2) -> Unit,
): (P1, P2) -> Unit {
    var job: Job? = null
    val executing = AtomicBoolean(false)

    return block@{ p1: P1, p2: P2 ->
        if (waitCompletion && !executing.get()) {
            return@block
        } else if (job?.isActive == true) {
            job?.cancel()
        }

        job = launch {
            delay(waitMs)

            try {
                executing.set(true)
                function(p1, p2)
            } finally {
                executing.set(false)
            }
        }
    }
}

// Re-binding the same function but with different amount of params
// Yes this is ugly but there isn't really a better way to do this efficiently afaik

inline fun CoroutineScope.debounce(waitMs: Long, waitCompletion: Boolean = false, crossinline function: suspend () -> Unit) =
    debounce<Any?, Any?>(waitMs, waitCompletion) { _, _ -> function() }
        .let { { it(null, null) } }

inline fun <T> CoroutineScope.debounce(waitMs: Long, waitCompletion: Boolean = false, crossinline function: suspend (T) -> Unit) =
    debounce<T, Any?>(waitMs, waitCompletion) { p1, _ -> function(p1) }
        .let { { p1: T -> it(p1, null) } }
