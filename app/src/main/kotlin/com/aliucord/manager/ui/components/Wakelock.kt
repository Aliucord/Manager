package com.aliucord.manager.ui.components

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import cafe.adriel.voyager.navigator.currentOrThrow

/**
 * Maintain an active screen wakelock as long as [active] is true and this component is in scope.
 */
@Composable
fun Wakelock(active: Boolean = false) {
    val window = LocalActivity.currentOrThrow.window
    DisposableEffect(active) {
        if (active) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
