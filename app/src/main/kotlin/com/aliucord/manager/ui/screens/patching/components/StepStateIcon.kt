package com.aliucord.manager.ui.screens.patching.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.aliucord.manager.R
import com.aliucord.manager.patcher.steps.base.StepState
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun StepStateIcon(
    state: StepState,
    size: Dp,
    stepProgress: Float = -1f,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stepProgress,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "Progress",
    )

    Crossfade(targetState = state, label = "State CrossFade") { animatedState ->
        when (animatedState) {
            StepState.Pending -> Icon(
                painter = painterResource(R.drawable.ic_circle),
                contentDescription = stringResource(R.string.status_queued),
                tint = MaterialTheme.colorScheme.onSurface.copy(.2f),
                modifier = Modifier.size(size)
            )

            StepState.Running -> {
                val strokeWidth = Dp(floor(size.value / 10) + 1)

                if (stepProgress > .05f) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        strokeWidth = strokeWidth,
                        modifier = Modifier
                            .size(size)
                            .semantics { contentDescription = "${(stepProgress * 100).roundToInt()}%" },
                    )
                } else {
                    val description = stringResource(R.string.status_ongoing)

                    // Infinite spinner
                    CircularProgressIndicator(
                        strokeWidth = strokeWidth,
                        modifier = Modifier
                            .size(size)
                            .semantics { contentDescription = description },
                    )
                }
            }

            StepState.Success -> Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = stringResource(R.string.status_success),
                tint = Color(0xFF59B463),
                modifier = Modifier.size(size)
            )

            StepState.Error -> Icon(
                painter = painterResource(R.drawable.ic_canceled),
                contentDescription = stringResource(R.string.status_failed),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(size)
            )

            StepState.Skipped -> Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = stringResource(R.string.status_skipped),
                tint = Color(0xFFAEAEAE),
                modifier = Modifier.size(size)
            )
        }
    }
}
