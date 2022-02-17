package com.aliucord.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
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
import com.aliucord.manager.ui.components.plugins.Changelog
import com.aliucord.manager.ui.components.plugins.PluginCard
import com.aliucord.manager.utils.Plugin

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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = search,
            onValueChange = { search = it },
            placeholder = { Text(stringResource(R.string.search)) },
            trailingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search_plugins)) },
            keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions { focusManager.clearFocus() },
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            )
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
                // TODO - make this get pluginEnabled from Aliucord preferences somehow
                var isChecked by remember { mutableStateOf(true) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var showChangelogDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.delete_plugin, plugin.manifest.name)) },
                    text = { Text(stringResource(R.string.delete_plugin_body)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                plugin.file.delete()
                                plugins = plugins.filter { it != plugin }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(stringResource(android.R.string.ok).uppercase())
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(stringResource(android.R.string.cancel).uppercase())
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