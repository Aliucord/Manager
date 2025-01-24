/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.screens.plugins.components.Changelog
import com.aliucord.manager.ui.screens.plugins.components.PluginCard

class PluginsScreen : Screen {
    override val key = "Plugins"

    @Composable
    override fun Content() {
        val model = getScreenModel<PluginsModel>()
        val plugins = model.filteredPlugins.collectAsState()

        // Refresh plugins list on activity resume or when this initially opens
        LifecycleResumeEffect(Unit) {
            model.refreshData()

            onPauseOrDispose {}
        }

        model.showUninstallDialog?.let { plugin ->
            UninstallPluginDialog(
                pluginName = plugin.manifest.name,
                onConfirm = { model.uninstallPlugin(plugin.path) },
                onDismiss = model::hideUninstallDialog
            )
        }

        model.showChangelogDialog?.let { plugin ->
            Changelog(
                plugin = plugin,
                onDismiss = model::hideChangelogDialog
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.plugins_title)) },
                    navigationIcon = { BackButton() },
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PluginSearch(
                    currentFilter = model.searchText.collectAsState().value,
                    onFilterChange = model::setSearchText,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (model.error) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(R.string.plugins_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else if (plugins.value.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 15.dp, top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = plugins.value) { plugin ->
                            PluginCard(
                                plugin = plugin,
                                onClickDelete = { model.showUninstallDialog(plugin) },
                                onClickShowChangelog = { model.showChangelogDialog(plugin) },
                                onSetEnabled = { model.setPluginEnabled(pluginName = plugin.manifest.name, it) }
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
                                painter = painterResource(R.drawable.ic_extension_off),
                                contentDescription = null
                            )
                            Text(stringResource(R.string.plugins_none_installed))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginSearch(
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = currentFilter,
        onValueChange = onFilterChange,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        label = { Text(stringResource(R.string.action_search)) },
        trailingIcon = {
            val isFilterBlank by remember { derivedStateOf { currentFilter.isEmpty() } }

            if (!isFilterBlank) {
                IconButton(onClick = { onFilterChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.plugins_search)
                )
            }
        },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions { focusManager.clearFocus() },
        modifier = modifier,
    )
}

@Composable
private fun UninstallPluginDialog(
    pluginName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_uninstall)
            )
        },
        title = {
            Text("${stringResource(R.string.action_uninstall)} $pluginName")
        },
        text = {
            Text(stringResource(R.string.plugins_delete_plugin_body))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
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
