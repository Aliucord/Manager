package com.aliucord.manager.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.SegmentedButton
import com.aliucord.manager.ui.util.DiscordVersion

@Composable
fun InstalledItemCard(
    appIcon: Painter,
    appName: String,
    packageName: String,
    discordVersion: DiscordVersion,
    onOpenApp: () -> Unit,
    onOpenInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.small,
        modifier = modifier.shadow(
            elevation = 5.dp,
            shape = MaterialTheme.shapes.medium,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                )

                Text(
                    text = "\"$appName\"",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .85f),
                    fontStyle = FontStyle.Italic,
                )
            }

            Column {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .9f),
                ) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("• ")
                            append(stringResource(R.string.label_version_discord))
                        }
                        append(" ")
                        if (discordVersion is DiscordVersion.Existing) {
                            append(discordVersion.name)
                            append(" - ")
                        }
                        append(discordVersion.toDisplayName())
                    })

                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("• ")
                            append(stringResource(R.string.label_pkg_name))
                            append(" ")
                        }
                        append(packageName)
                    })

                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("• ")
                            append(stringResource(R.string.label_base_updated))
                            append(": ")
                        }
                        append("Yes")
                    })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clip(MaterialTheme.shapes.large),
            ) {
                SegmentedButton(
                    icon = painterResource(R.drawable.ic_delete_forever),
                    text = stringResource(R.string.action_uninstall),
                    onClick = onUninstall
                )
                SegmentedButton(
                    icon = painterResource(R.drawable.ic_info),
                    text = stringResource(R.string.action_open_info),
                    onClick = onOpenInfo
                )
                SegmentedButton(
                    icon = painterResource(R.drawable.ic_launch),
                    text = stringResource(R.string.action_launch),
                    onClick = onOpenApp
                )
            }
        }
    }
}
