package com.aliucord.manager.ui.components

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.aliucord.manager.R
import com.aliucord.manager.ui.util.DiscordVersion

@Composable
fun AnimatedVersionDisplay(
    version: DiscordVersion,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        enter = fadeIn() + slideInVertically { it * -2 },
        exit = fadeOut() + slideOutVertically { it * -2 },
        visible = version !is DiscordVersion.None,
        modifier = modifier,
    ) {
        VersionDisplay(
            version = version,
            prefix = {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.version_supported))
                    append(" ")
                }
            },
            modifier = Modifier.alpha(.5f),
        )
    }
}
