package com.aliucord.manager.ui.screens.install.components

import androidx.compose.animation.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TimeElapsed(
    seconds: Float,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = enabled,
        enter = fadeIn(),
        exit = ExitTransition.None,
        label = "TimeElapsed Visibility"
    ) {
        Text(
            text = "%.2fs".format(seconds),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = modifier,
        )
    }
}
