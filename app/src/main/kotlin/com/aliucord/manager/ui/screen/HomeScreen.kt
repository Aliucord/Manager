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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.aliucord.manager.R
import com.aliucord.manager.model.github.Commit
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.component.installer.DownloadMethod
import com.aliucord.manager.ui.component.installer.InstallerDialog
import com.aliucord.manager.ui.navigation.AppDestination
import com.aliucord.manager.ui.viewmodel.HomeViewModel
import com.aliucord.manager.util.Github
import com.xinto.taxi.BackstackNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navigator: BackstackNavigator<AppDestination>) {
    val viewModel: HomeViewModel = viewModel()

    var showOptionsDialog by remember { mutableStateOf(false) }

    if (showOptionsDialog) {
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            navigator.push(AppDestination.Install)
        }

        InstallerDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = { method ->
                if (method == DownloadMethod.DOWNLOAD) {
                    navigator.push(AppDestination.Install)
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
                        "Aliucord${Prefs.packageName.get().let { if (it != "com.aliucord") " ($it)" else "" }}",
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
                                    navigator.push(AppDestination.Install)
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
                    }

                    if (viewModel.installedVersion != "-") {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.uninstallAliucord()
                                    }
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Uninstall"
                                    )
                                }

                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = viewModel::launchAliucord
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        painter = painterResource(R.drawable.ic_launch_24dp),
                                        contentDescription = "Launch"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val pager = remember {
            Pager(
                PagingConfig(
                    pageSize = 30,
                    enablePlaceholders = true,
                    maxSize = 200
                )
            ) {
                object : PagingSource<Int, Commit>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Commit> {
                        val pageNumber = params.key ?: 0

                        val response = Github.getCommits("page" to pageNumber.toString())
                        val prevKey = if (pageNumber > 0) pageNumber - 1 else null
                        val nextKey = if (response.isNotEmpty()) pageNumber + 1 else null

                        return LoadResult.Page(
                            data = response,
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    }

                    override fun getRefreshKey(state: PagingState<Int, Commit>) = state.anchorPosition?.let {
                        state.closestPageToPosition(it)?.prevKey?.plus(1) ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
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
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.commits),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.weight(1f, true))
                }
                val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (lazyPagingItems.loadState.refresh == LoadState.Loading) item {
                        Text(
                            text = stringResource(R.string.paging_initial_load),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    items(lazyPagingItems) { commitData ->
                        if (commitData == null) return@items

                        val localUriHandler = LocalUriHandler.current

                        ListItem(
                            modifier = Modifier.clickable { localUriHandler.openUri(commitData.htmlUrl) },
                            overlineText = { Text(commitData.sha.substring(0, 7)) },
                            headlineText = { Text("${commitData.commit.message.split("\n").first()} - ${commitData.author.name}") }
                        )
                    }

                    if (lazyPagingItems.loadState.append == LoadState.Loading) item {
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
