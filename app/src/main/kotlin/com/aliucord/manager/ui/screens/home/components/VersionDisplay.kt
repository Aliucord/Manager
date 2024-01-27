package com.aliucord.manager.ui.screens.home.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import com.aliucord.manager.ui.util.DiscordVersion

@Composable
fun VersionDisplay(
    version: DiscordVersion,
    prefix: (@Composable AnnotatedString.Builder.() -> Unit)? = null,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            if (prefix != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    prefix()
                }
            }
            if (version is DiscordVersion.Existing) {
                append(version.name)
                append(" - ")
            }
            append(version.toDisplayName())
        },
        style = style,
        modifier = modifier,
    )
}
