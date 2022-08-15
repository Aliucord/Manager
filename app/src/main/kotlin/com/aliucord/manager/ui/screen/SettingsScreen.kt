/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = getViewModel(),
    onClickBack: () -> Unit
) {
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val preferences = viewModel.preferences

            if (viewModel.showThemeDialog) {
                ThemeDialog(
                    currentTheme = preferences.theme,
                    onDismissRequest = viewModel::hideThemeDialog,
                    onConfirm = viewModel::setTheme
                )
            }

            ListItem(
                modifier = Modifier.clickable(onClick = viewModel::showThemeDialog),
                headlineText = { Text(stringResource(R.string.theme)) },
                supportingText = { Text(stringResource(R.string.theme_setting_description)) },
                trailingContent = {
                    FilledTonalButton(onClick = viewModel::showThemeDialog) {
                        Text(preferences.theme.displayName)
                    }
                }
            )

            SwitchSetting(
                checked = preferences.dynamicColor,
                title = { Text(stringResource(R.string.dynamic_color)) },
                onCheckedChange = { preferences.dynamicColor = it }
            )

            Divider()

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                value = preferences.appName,
                onValueChange = viewModel::setAppName,
                label = { Text(stringResource(R.string.app_name_setting)) },
                placeholder = { Text("Aliucord") },
                singleLine = true
            )

            SwitchSetting(
                checked = preferences.replaceBg,
                title = { Text(stringResource(R.string.replace_bg)) },
                onCheckedChange = { preferences.replaceBg = it }
            )

            SwitchSetting(
                checked = preferences.devMode,
                title = { Text(stringResource(R.string.advanced_options)) },
                onCheckedChange = { preferences.devMode = it }
            )

            AnimatedVisibility(
                visible = preferences.devMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        value = preferences.packageName,
                        onValueChange = viewModel::setPackageName,
                        label = { Text(stringResource(R.string.package_name)) },
                        placeholder = { Text("com.aliucord") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        value = preferences.version,
                        onValueChange = viewModel::setVersion,
                        label = { Text(stringResource(R.string.version)) },
                        singleLine = true
                    )

                    SwitchSetting(
                        checked = preferences.debuggable,
                        title = { Text(stringResource(R.string.debuggable)) },
                        description = { Text(stringResource(R.string.debuggable_description)) },
                        onCheckedChange = { preferences.debuggable = it }
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = viewModel::clearCacheDir
            ) {
                Text(
                    text = stringResource(R.string.clear_files_cache),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchSetting(
    checked: Boolean,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        leadingContent = icon,
        headlineText = title,
        supportingText = description,
        trailingContent = {
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun ThemeDialog(
    currentTheme: Theme,
    onDismissRequest: () -> Unit,
    onConfirm: (Theme) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.Style,
                contentDescription = stringResource(R.string.theme)
            )
        },
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                Theme.values().forEach { theme ->
                    Row(
                        modifier = Modifier.clickable { selectedTheme = theme },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(Modifier.weight(1f, true))

                        RadioButton(
                            selected = theme == selectedTheme,
                            onClick = { selectedTheme = theme }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedTheme)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}
