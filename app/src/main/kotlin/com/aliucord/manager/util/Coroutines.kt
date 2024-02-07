@file:Suppress("NOTHING_TO_INLINE")

package com.aliucord.manager.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Launch a Job for a block without returning anything
 */
inline fun CoroutineScope.launchBlock(
    context: CoroutineContext = Dispatchers.Main,
    noinline block: suspend CoroutineScope.() -> Unit,
) {
    launch(context, block = block)
}

/**
 * Wrapper util to run a block with the main thread context
 */
suspend inline fun mainThread(noinline block: CoroutineScope.() -> Unit) =
    withContext(Dispatchers.Main, block)
