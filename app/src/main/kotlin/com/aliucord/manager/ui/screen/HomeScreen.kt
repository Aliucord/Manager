/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.*
import com.aliucord.manager.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = getViewModel(),
    onClickInstall: (InstallData) -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }

    if (showOptionsDialog) {
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            onClickInstall(
                InstallData(
                    DownloadMethod.SELECT,
                )
            )
        }

        InstallerDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = { data ->
                when (data.downloadMethod) {
                    DownloadMethod.SELECT -> filePicker.launch(arrayOf("application/octet-stream"))
                    DownloadMethod.DOWNLOAD -> onClickInstall(data)
                }
            }
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedCard {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        text = "Aliucord${viewModel.preferences.packageName.let { if (it != "com.aliucord") " ($it)" else "" }}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        buildAnnotatedString {
                            append("Supported version: ")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(viewModel.supportedVersion)
                            }

                            append("\nInstalled version: ")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(viewModel.installedVersion)
                            }
                        }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val (icon, description) = when (viewModel.installedVersion) {
                        "-" -> Icons.Default.Download to R.string.install
                        viewModel.supportedVersion -> Icons.Default.Update to R.string.reinstall
                        else -> Icons.Default.Update to R.string.update
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f, true),
                            onClick = {
                                if (viewModel.preferences.devMode) {
                                    showOptionsDialog = true
                                } else {
                                    onClickInstall(
                                        InstallData(
                                            DownloadMethod.DOWNLOAD,
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = icon,
                                contentDescription = stringResource(description),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )

                            Text(stringResource(description))
                        }
                    }

                    if (viewModel.installedVersion != "-") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = viewModel::uninstallAliucord
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.uninstall)
                                    )
                                }

                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = viewModel::launchAliucord
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        imageVector = Icons.Default.Launch,
                                        contentDescription = stringResource(R.string.launch)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, true)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val lazyPagingItems = viewModel.commits.collectAsLazyPagingItems()

                Text(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp),
                    text = stringResource(R.string.commits),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                        item {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally),
                                text = stringResource(R.string.paging_initial_load)
                            )
                        }
                    }

                    items(lazyPagingItems) { commitData ->
                        if (commitData == null) return@items

                        val uriHandler = LocalUriHandler.current

                        ListItem(
                            modifier = Modifier.clickable { uriHandler.openUri(commitData.htmlUrl) },
                            overlineText = { Text(commitData.sha.substring(0, 7)) },
                            headlineText = { Text("${commitData.commit.message.split("\n").first()} - ${commitData.author?.name ?: "UNKNOWN"}") }
                        )
                    }

                    if (lazyPagingItems.loadState.append == LoadState.Loading) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}
