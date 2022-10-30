/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aliucord.manager.R
import com.aliucord.manager.ui.screen.InstallData

enum class DownloadMethod { DOWNLOAD }
enum class DiscordType { KOTLIN, REACT_NATIVE }

@Composable
fun InstallerDialog(
    onDismiss: () -> Unit,
    onConfirm: (InstallData) -> Unit
) {
    val downloadMethod by rememberSaveable { mutableStateOf(DownloadMethod.DOWNLOAD) }
    var discordType by rememberSaveable { mutableStateOf<DiscordType?>(null) }

    fun triggerConfirm() {
        onDismiss()
        onConfirm(
            InstallData(
                downloadMethod,
                discordType ?: return
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            shape = AlertDialogDefaults.shape,
        ) {
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = 380.dp)
                    .padding(24.dp)
            ) {
                // Icon
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        tint = AlertDialogDefaults.iconContentColor,
                        contentDescription = null
                    )
                }

                // Title
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 30.dp)
                ) {
                    Text(
                        stringResource(R.string.selector_discord_type),
                        color = AlertDialogDefaults.titleContentColor,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                // Body
                Box(
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                ) {
                    Text(
                        stringResource(R.string.selector_discord_type_body),
                        textAlign = TextAlign.Center,
                        color = AlertDialogDefaults.textContentColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                ) {
                    ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                        Button(
                            shape = MaterialTheme.shapes.extraSmall,
                            onClick = {
                                discordType = DiscordType.REACT_NATIVE
                                triggerConfirm()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(stringResource(R.string.selector_discord_type_kotlin))
                        }
                        Button(
                            shape = MaterialTheme.shapes.extraSmall,
                            onClick = {
                                discordType = DiscordType.KOTLIN
                                triggerConfirm()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(stringResource(R.string.selector_discord_type_old))
                        }
                    }
                }
            }
        }
    }
}
