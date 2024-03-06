package com.aliucord.manager.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.aliucord.manager.util.findActivity

/**
 * Maintain an active screen wakelock as long as [active] is true and this component is in scope.
 */
@Composable
fun Wakelock(active: Boolean = false) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val window = context.findActivity()?.window

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
