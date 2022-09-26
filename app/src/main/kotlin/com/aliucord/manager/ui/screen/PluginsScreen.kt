/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.plugins.Changelog
import com.aliucord.manager.ui.component.plugins.PluginCard
import com.aliucord.manager.ui.viewmodel.PluginsViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    viewModel: PluginsViewModel = getViewModel()
) {
    if (viewModel.showUninstallDialog) {
        val plugin = viewModel.selectedPlugin!!

        AlertDialog(
            onDismissRequest = viewModel::hideUninstallDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.uninstall)
                )
            },
            title = {
                Text("${stringResource(R.string.uninstall)} ${plugin.manifest.name}")
            },
            text = {
                Text(stringResource(R.string.delete_plugin_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uninstallPlugin(plugin)
                        viewModel.hideUninstallDialog()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                Button(
                    onClick = viewModel::hideUninstallDialog,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (viewModel.showChangelogDialog) {
        Changelog(
            plugin = viewModel.selectedPlugin!!,
            onDismiss = viewModel::hideChangelogDialog
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val search = viewModel.search

        if (viewModel.plugins.isNotEmpty()) {
            val focusManager = LocalFocusManager.current

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                value = viewModel.search,
                onValueChange = viewModel::search,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(stringResource(R.string.search)) },
                trailingIcon = {
                    if (viewModel.search.isNotBlank()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_plugins)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions { focusManager.clearFocus() }
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    viewModel.plugins.filter { plugin ->
                        plugin.manifest.run {
                            name.contains(search, true)
                                || description.contains(search, true)
                                || authors.any { (name) -> name.contains(search, true) }
                        }
                    }
                ) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        onClickDelete = { viewModel.showUninstallDialog(plugin) },
                        onClickShowChangelog = { viewModel.showChangelogDialog(plugin) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null
                    )
                    Text(stringResource(R.string.no_plugins_installed))
                }
            }
        }
    }
}
