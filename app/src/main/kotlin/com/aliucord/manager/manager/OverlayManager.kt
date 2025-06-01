package com.aliucord.manager.manager

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

typealias ResultComposable<R> = @Composable (onResult: (R) -> Unit) -> Unit

/**
 * This is used to display dialogs on top of the current activity at any point in the code.
 * The main use case for this is dialogs for which a result is needed during patching steps.
 *
 * The only other alternative to this setup is binding `Flow`s from the steps back to the patching screen model
 * and then displaying them in the UI, which is too much boilerplate.
 */
@Stable
class OverlayManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Main) + SupervisorJob()
    private var overlays = mutableStateListOf<ResultComposable<Any?>>()
    private var overlayResults = MutableSharedFlow<Pair<ResultComposable<Any?>, Any?>>(extraBufferCapacity = 5)

    /**
     * Display all the currently queued overlays.
     */
    @Composable
    fun Overlays() {
        for (composable in overlays) {
            key(System.identityHashCode(composable)) {
                val composable by rememberUpdatedState(composable)

                composable { result ->
                    if (!overlayResults.tryEmit(composable to result))
                        error("overlayResults flow full!")

                    overlays -= composable
                }
            }
        }
    }

    /**
     * Adds a composable to the overlay stack which will be displayed over the top of any content.
     *
     * This content will be displayed until the `onResult` callback is called,
     * after which this method will finish suspending with the result from the invoked callback.
     *
     * If the coroutine scope this method was called in gets cancelled, then the overlay will be
     * removed and no result will be returned (cancelled).
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <R> startComposableForResult(composable: ResultComposable<R>): R {
        return suspendCancellableCoroutine { continuation ->
            val job = overlayResults
                .filter { (c, _) -> c === composable }
                .onEach { (_, result) -> continuation.resume(result as R) }
                .cancellable()
                .launchIn(coroutineScope)

            continuation.invokeOnCancellation {
                coroutineScope.launch {
                    overlays -= composable
                    job.cancel()
                }
            }

            coroutineScope.launch {
                overlays += composable
            }
        }
    }
}
