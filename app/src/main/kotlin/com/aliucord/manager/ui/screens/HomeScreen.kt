/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.R
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.components.PluginsList
import com.aliucord.manager.ui.components.installer.DownloadMethod
import com.aliucord.manager.ui.components.installer.InstallerDialog
import com.aliucord.manager.ui.screens.destinations.CommitsScreenDestination
import com.aliucord.manager.ui.screens.destinations.InstallerScreenDestination
import com.aliucord.manager.ui.viewmodels.HomeViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val viewModel: HomeViewModel = viewModel()

    var showOptionsDialog by remember { mutableStateOf(false) }

    if (showOptionsDialog) {
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            navigator.navigate(InstallerScreenDestination)
        }

        InstallerDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = { method ->
                if (method == DownloadMethod.DOWNLOAD) {
                    navigator.navigate(InstallerScreenDestination())
                } else {
                    filePicker.launch(arrayOf("application/octet-stream"))
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        "Aliucord${Prefs.packageName.get().let { if (it != "com.aliucord") " ($it)" else ""}}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(buildAnnotatedString {
                        append("Supported version: ")

                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(viewModel.supportedVersion)
                        }

                        append("\nInstalled version: ")

                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(viewModel.installedVersion)
                        }
                    })
                }

                val (drawable, description) = when (viewModel.installedVersion) {
                    "-" -> R.drawable.ic_download_24dp to R.string.install
                    viewModel.supportedVersion -> R.drawable.ic_reinstall_24dp to R.string.reinstall
                    else -> R.drawable.ic_update_24dp to R.string.update
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f, true),
                            onClick = {
                                if (Prefs.devMode.get()) {
                                    showOptionsDialog = true
                                } else {
                                    navigator.navigate(InstallerScreenDestination())
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                painter = painterResource(drawable),
                                contentDescription = stringResource(description),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )

                            Text(stringResource(description))
                        }

                        if (viewModel.installedVersion == "-") Button(
                            modifier = Modifier.wrapContentSize(),
                            onClick = { navigator.navigate(CommitsScreenDestination) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                painter = painterResource(R.drawable.ic_update_24dp),
                                contentDescription = "Commits",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (viewModel.installedVersion != "-") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                onClick = {
                                    viewModel.uninstallAliucord()
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(8.dp),
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Uninstall",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = viewModel::launchAliucord,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    modifier = Modifier.padding(8.dp),
                                    painter = painterResource(R.drawable.ic_launch_24dp),
                                    contentDescription = "Launch",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { navigator.navigate(CommitsScreenDestination) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    modifier = Modifier.padding(8.dp),
                                    painter = painterResource(R.drawable.ic_update_24dp),
                                    contentDescription = "Commits",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plugins",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.weight(1f, true))
                }

                PluginsList()
            }
        }
    }
}
