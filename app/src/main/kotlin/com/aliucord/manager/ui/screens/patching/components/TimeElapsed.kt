package com.aliucord.manager.ui.screens.patching.components

import androidx.compose.animation.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R

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
            text = stringResource(R.string.time_elapsed_seconds, seconds),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = modifier,
        )
    }
}
