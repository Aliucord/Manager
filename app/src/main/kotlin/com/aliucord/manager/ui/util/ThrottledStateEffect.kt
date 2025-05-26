package com.aliucord.manager.ui.util

import androidx.compose.runtime.*
import kotlinx.coroutines.*

@Composable
fun <T> throttledState(
    value: T,
    throttleMs: Long,
): State<T> {
    val scope = rememberCoroutineScope()
    val state = remember { mutableStateOf(value) }
    var lastEmitTime by remember { mutableLongStateOf(0L) }
    var trailingJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(value) {
        val now = System.currentTimeMillis()
        val sinceLastEmit = now - lastEmitTime

        if (sinceLastEmit >= throttleMs) {
            lastEmitTime = now
            state.value = value
        } else {
            trailingJob?.cancel()
            trailingJob = scope.launch {
                delay(throttleMs - sinceLastEmit)
                lastEmitTime = System.currentTimeMillis()
                state.value = value
            }
        }
    }

    return state
}
