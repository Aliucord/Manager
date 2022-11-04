package com.aliucord.manager.ui.component.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.ui.viewmodel.HomeViewModel.VersionType

@Composable
fun InfoCard(
    packageName: String,
    supportedVersion: String,
    supportedVersionType: VersionType,
    currentVersion: String,
    currentVersionType: VersionType,
    onDownloadClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onLaunchClick: () -> Unit
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
                        if (supportedVersion.isNotEmpty()) {
                            append(supportedVersion)
                            append(" - ")
                        }
                        append(supportedVersionType.toDisplayName())
                    }

                    append('\n')
                    append(stringResource(R.string.version_installed))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(" ")
                        if (currentVersion.isNotEmpty()) {
                            append(currentVersion)
                            append(" - ")
                        }
                        append(currentVersionType.toDisplayName())
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
                val (icon, description) = when (currentVersion) {
                    "" -> Icons.Default.Download to R.string.action_install
                    supportedVersion -> Icons.Default.Refresh to R.string.action_reinstall
                    else -> Icons.Default.Update to R.string.action_update
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    onClick = onDownloadClick,
                    shape = ShapeDefaults.Large,
                    enabled = supportedVersionType != VersionType.ERROR
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(description)
                        )
                        if (!currentVersionType.isVersion()) {
                            Text(
                                stringResource(description),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (currentVersionType.isVersion()) {
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
                            imageVector = Icons.Default.Launch,
                            contentDescription = stringResource(R.string.action_launch)
                        )
                    }
                }
            }
        }
    }
}
