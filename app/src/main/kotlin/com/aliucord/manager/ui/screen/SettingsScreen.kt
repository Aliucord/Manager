/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.settings.*
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = getViewModel()
) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
    ) {
        val preferences = viewModel.preferences

        if (viewModel.showThemeDialog) {
            ThemeDialog(
                currentTheme = preferences.theme,
                onDismissRequest = viewModel::hideThemeDialog,
                onConfirm = viewModel::setTheme
            )
        }

        SettingsHeader(stringResource(R.string.appearance))

        SettingsItem(
            modifier = Modifier.clickable(onClick = viewModel::showThemeDialog),
            icon = { Icon(Icons.Default.Style, null) },
            text = { Text(stringResource(R.string.theme)) }
        ) {
            FilledTonalButton(onClick = viewModel::showThemeDialog) {
                Text(preferences.theme.displayName)
            }
        }

        SettingsSwitch(
            label = stringResource(R.string.dynamic_color),
            pref = preferences.dynamicColor,
            icon = { Icon(Icons.Default.Palette, null) }
        ) {
            preferences.dynamicColor = it
        }

        SettingsHeader(stringResource(R.string.advanced))

        SettingsSwitch(
            label = stringResource(R.string.replace_bg),
            secondaryLabel = stringResource(R.string.replace_bg_description),
            pref = preferences.replaceIcon,
            icon = { Icon(Icons.Default.AppShortcut, null) }
        ) {
            preferences.replaceIcon = it
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsTextField(
            label = stringResource(R.string.app_name_setting),
            pref = preferences.appName,
            onPrefChange = viewModel::setAppName
        )

        SettingsSwitch(
            label = stringResource(R.string.developer_options),
            pref = preferences.devMode,
            icon = { Icon(Icons.Default.Code, null) }
        ) {
            preferences.devMode = it
        }

        AnimatedVisibility(
            visible = preferences.devMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsTextField(
                    label = stringResource(R.string.package_name),
                    pref = preferences.packageName,
                    onPrefChange = viewModel::setPackageName
                )

                SettingsTextField(
                    label = stringResource(R.string.version),
                    pref = preferences.version,
                    onPrefChange = viewModel::setVersion
                )

                SettingsSwitch(
                    label = stringResource(R.string.debuggable),
                    secondaryLabel = stringResource(R.string.debuggable_description),
                    pref = preferences.debuggable,
                    icon = { Icon(Icons.Default.BugReport, null) }
                ) {
                    preferences.debuggable = it
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = ShapeDefaults.Large,
            onClick = viewModel::clearCacheDir
        ) {
            Text(
                text = stringResource(R.string.clear_cache),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ThemeDialog(
    currentTheme: Theme,
    onDismissRequest: () -> Unit,
    onConfirm: (Theme) -> Unit
) {
    var selectedTheme by rememberSaveable { mutableStateOf(currentTheme) }

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
                        modifier = Modifier
                            .clickable { selectedTheme = theme }
                            .padding(start = 8.dp),
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
