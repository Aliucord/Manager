package com.aliucord.manager.ui.screens.install.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.base.StepState
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun StepStatusIcon(
    status: StepState,
    size: Dp,
    stepProgress: Float = -1f,
) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)
    val context = LocalContext.current

    when (status) {
        StepState.Pending -> Icon(
            painter = painterResource(R.drawable.ic_circle),
            contentDescription = stringResource(R.string.status_queued),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
            modifier = Modifier.size(size)
        )

        StepState.Running -> {
            val animatedProgress by animateFloatAsState(
                targetValue = stepProgress,
                animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                label = "Progress",
            )

            if (stepProgress > .1f) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    strokeWidth = strokeWidth,
                    modifier = Modifier
                        .size(size)
                        .semantics { contentDescription = "${(stepProgress * 100).roundToInt()}%" },
                )
            } else {
                // Infinite spinner
                CircularProgressIndicator(
                    strokeWidth = strokeWidth,
                    modifier = Modifier
                        .size(size)
                        .semantics { contentDescription = context.getString(R.string.status_ongoing) },
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
