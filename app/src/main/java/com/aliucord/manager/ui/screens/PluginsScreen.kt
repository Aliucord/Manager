package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.Changelog
import com.aliucord.manager.utils.Plugin

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun PluginsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        var plugins by remember { mutableStateOf(Plugin.loadAll()) }
        var search by remember { mutableStateOf("") }

        TextField(
            value = search,
            placeholder = { Text(stringResource(R.string.search)) },
            onValueChange = { search = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyColumn {
            items(plugins.filter {
                it.manifest.run {
                    name.contains(search, true)
                            || description.contains(search, true)
                            || authors.any { a -> a.name.contains(search, true) }
                }
            }) { p ->
                // TODO - make this get pluginEnabled from Aliucord preferences somehow
                val isChecked = remember { mutableStateOf(true) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                val showChangelogDialog = remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(R.string.delete_plugin, p.manifest.name)) },
                        text = { Text(stringResource(R.string.delete_plugin_body)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    p.file.delete()
                                    plugins = plugins.filter { it != p }
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error, contentColor = MaterialTheme.colors.onError)
                            ) {
                                Text(stringResource(android.R.string.ok).uppercase())
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                            ) {
                                Text(stringResource(android.R.string.cancel).uppercase())
                            }
                        }
                    )
                }

                if (showChangelogDialog.value) {
                    Dialog(
                        onDismissRequest = { showChangelogDialog.value = false },
                        content = {
                            Changelog(p, showChangelogDialog)
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = 1.dp
                ) {
                    Column {
                        ListItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.plugin_title,
                                        p.manifest.name,
                                        p.manifest.version,
                                        p.manifest.authors.joinToString(
                                            " ",
                                            transform = { it.name })
                                    ),
                                )
                            },
                            trailing = {
                                Switch(
                                    checked = isChecked.value,
                                    onCheckedChange = { isChecked.value = it },
                                )
                            },
                        )
                        Divider(color = MaterialTheme.colors.primary)
                        Text(
                            p.manifest.description,
                            modifier = Modifier
                                .heightIn(min = 50.dp)
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
                        )

                        Row(modifier = Modifier.align(Alignment.End)) {
                            if (p.manifest.changelog != null) {
                                IconButton(onClick = { showChangelogDialog.value = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_history_white_24dp),
                                        contentDescription = stringResource(
                                            R.string.view_plugin_changelog,
                                            p.manifest.name
                                        )
                                    )
                                }
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(
                                        R.string.delete_plugin,
                                        p.manifest.name
                                    ),
                                    tint = MaterialTheme.colors.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
