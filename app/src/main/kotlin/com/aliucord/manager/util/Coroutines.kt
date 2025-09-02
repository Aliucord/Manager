@file:Suppress("NOTHING_TO_INLINE")

package com.aliucord.manager.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Launches a Job in a fixed-size CPU-bound thread pool.
 * Used for heavy or intensive CPU-bound tasks.
 */
inline fun CoroutineScope.launchBlock(
    context: CoroutineContext = Dispatchers.Default,
    noinline block: suspend CoroutineScope.() -> Unit,
) {
    launch(context, block = block)
}

/**
 * Launches a Job on a background thread in a dynamically sized thread pool.
 * Used for IO or other lightweight tasks that spend most of their time waiting.
 */
inline fun CoroutineScope.launchIO(noinline block: suspend CoroutineScope.() -> Unit) =
    launchBlock(Dispatchers.IO, block)

/**
 * Utility wrapper around [withContext] to switch to the main thread for the [block].
 */
suspend inline fun <T> mainThread(noinline block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)
