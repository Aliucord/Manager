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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.plugins.Changelog
import com.aliucord.manager.ui.components.plugins.PluginCard

class PluginsScreen : Screen {
    override val key = "Plugins"

    @Composable
    override fun Content() {
        val model = getScreenModel<PluginsModel>()

        if (model.showUninstallDialog) {
            val plugin = model.selectedPlugin!!

            UninstallPluginDialog(
                pluginName = plugin.manifest.name,
                onConfirm = { model.uninstallPlugin(plugin) },
                onDismiss = model::hideUninstallDialog
            )
        }

        if (model.showChangelogDialog) {
            Changelog(
                plugin = model.selectedPlugin!!,
                onDismiss = model::hideChangelogDialog
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PluginSearch(
                currentFilter = model.search,
                onFilterChange = model::search,
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
            } else if (model.plugins.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 15.dp, top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        // TODO: remember {} this
                        model.plugins.filter { plugin ->
                            plugin.manifest.run {
                                name.contains(model.search, true)
                                    || description.contains(model.search, true)
                                    || authors.any { (name) -> name.contains(model.search, true) }
                            }
                        }
                    ) { plugin ->
                        PluginCard(
                            plugin = plugin,
                            enabled = model.enabled[plugin.manifest.name] ?: true,
                            onClickDelete = { model.showUninstallDialog(plugin) },
                            onClickShowChangelog = { model.showChangelogDialog(plugin) },
                            onSetEnabled = { model.setPluginEnabled(plugin.manifest.name, it) }
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
        keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search),
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
