package com.aliucord.manager.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.ui.util.DiscordVersion

@Composable
fun InfoCard(
    packageName: String,
    supportedVersion: DiscordVersion,
    currentVersion: DiscordVersion,
    onDownloadClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onLaunchClick: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${stringResource(R.string.aliucord)} ($packageName)",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.version_supported))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(" ")
                        if (supportedVersion is DiscordVersion.Existing) {
                            append(supportedVersion.name)
                            append(" - ")
                        }
                        append(supportedVersion.toDisplayName())
                    }

                    append('\n')
                    append(stringResource(R.string.version_installed))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(" ")
                        if (currentVersion is DiscordVersion.Existing) {
                            append(currentVersion.name)
                            append(" - ")
                        }
                        append(currentVersion.toDisplayName())
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val (icon, description) = when {
                    currentVersion !is DiscordVersion.Existing ->
                        R.drawable.ic_download to R.string.action_install

                    currentVersion < supportedVersion ->
                        R.drawable.ic_refresh to R.string.action_reinstall

                    else ->
                        R.drawable.ic_update to R.string.action_update
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    onClick = onDownloadClick,
                    shape = ShapeDefaults.Large,
                    enabled = supportedVersion !is DiscordVersion.Error
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = stringResource(description)
                        )
                        if (currentVersion is DiscordVersion.None) {
                            Text(
                                stringResource(description),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (currentVersion is DiscordVersion.Existing) {
                    FilledTonalIconButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        onClick = onUninstallClick,
                        shape = ShapeDefaults.Large
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_uninstall)
                        )
                    }

                    FilledTonalIconButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        onClick = onLaunchClick,
                        shape = ShapeDefaults.Large
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launch),
                            contentDescription = stringResource(R.string.action_launch)
                        )
                    }
                }
            }
        }
    }
}
