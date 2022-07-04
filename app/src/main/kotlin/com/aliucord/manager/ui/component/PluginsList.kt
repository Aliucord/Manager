/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aliucord.manager.R
import com.aliucord.manager.model.Plugin
import com.aliucord.manager.ui.component.plugins.Changelog
import com.aliucord.manager.ui.component.plugins.PluginCard

@Composable
fun PluginsList() = Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    var plugins by remember { mutableStateOf(Plugin.loadAll()) }
    val focusManager = LocalFocusManager.current
    var search by remember { mutableStateOf("") }

    if (plugins.isNotEmpty()) {
        OutlinedTextField(
            maxLines = 1,
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search)) },
            trailingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search_plugins)) },
            keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions { focusManager.clearFocus() },
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(plugins.filter { plugin ->
                plugin.manifest.run {
                    name.contains(search, true)
                        || description.contains(search, true)
                        || authors.any { a -> a.name.contains(search, true) }
                }
            }) { plugin ->
                var showDeleteDialog by remember { mutableStateOf(false) }
                var showChangelogDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.uninstall_plugin, plugin.manifest.name)
                        )
                    },
                    title = { Text(stringResource(R.string.uninstall_plugin, plugin.manifest.name)) },
                    text = { Text(stringResource(R.string.delete_plugin_body)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                plugin.file.delete()
                                plugins = plugins.filter { it != plugin }
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDeleteDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                )

                if (showChangelogDialog) Dialog(
                    onDismissRequest = { showChangelogDialog = false },
                    content = { Changelog(plugin, onDismiss = { showChangelogDialog = false }) }
                )

                PluginCard(
                    plugin = plugin,
                    onDelete = { showDeleteDialog = true },
                    onShowChangelog = { showChangelogDialog = true }
                )
            }
        }
    } else {
        Text(stringResource(R.string.no_plugins_installed))
    }
}
