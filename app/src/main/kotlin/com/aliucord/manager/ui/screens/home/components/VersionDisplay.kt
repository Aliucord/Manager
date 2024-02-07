package com.aliucord.manager.ui.screens.home.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
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
            prefix?.invoke(this)

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
