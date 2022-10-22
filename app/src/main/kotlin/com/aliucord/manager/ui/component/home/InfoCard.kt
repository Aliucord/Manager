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

@Composable
fun InfoCard(
    packageName: String,
    supportedVersion: String,
    currentVersion: String,
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
                text = "Aliucord${packageName.let { if (it != "com.aliucord") " ($it)" else "" }}",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                buildAnnotatedString {
                    append("Supported version: ")

                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(supportedVersion)
                    }

                    append("\nInstalled version: ")

                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentVersion)
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
                    "-" -> Icons.Default.Download to R.string.action_install
                    supportedVersion -> Icons.Default.Refresh to R.string.action_reinstall
                    else -> Icons.Default.Update to R.string.action_update
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    onClick = onDownloadClick,
                    shape = ShapeDefaults.Large
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(description)
                        )
                        if (currentVersion == "-") Text(
                            stringResource(description),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (currentVersion != "-") {
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
