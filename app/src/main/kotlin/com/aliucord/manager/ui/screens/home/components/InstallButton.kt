package com.aliucord.manager.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.util.thenIf
import com.valentinilk.shimmer.*

private val shimmerTheme = defaultShimmerTheme.copy(
    shimmerWidth = 150.dp,
    animationSpec = infiniteRepeatable(
        animation = shimmerSpec(
            durationMillis = 2000,
            easing = LinearEasing,
            delayMillis = 3500,
        ),
        repeatMode = RepeatMode.Restart,
        initialStartOffset = StartOffset(1000),
    ),
    blendMode = BlendMode.Lighten,
    shaderColors = listOf(
        Color.White.copy(alpha = 0.00f),
        Color.White.copy(alpha = 0.50f),
        Color.White.copy(alpha = 1.00f),
        Color.White.copy(alpha = 0.50f),
        Color.White.copy(alpha = 0.00f),
    ),
    shaderColorStops = listOf(
        0.0f,
        0.25f,
        0.5f,
        0.75f,
        1.0f,
    ),
)

@Composable
fun InstallButton(
    enabled: Boolean = true,
    secondaryInstall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalShimmerTheme provides shimmerTheme
    ) {
        FilledTonalIconButton(
            shape = RectangleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (secondaryInstall) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
            enabled = enabled,
            onClick = onClick,
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .thenIf(!secondaryInstall) { shimmer() }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.action_add_install),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
