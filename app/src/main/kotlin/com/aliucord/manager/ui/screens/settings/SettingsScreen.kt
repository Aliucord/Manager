/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.components.Theme
import com.aliucord.manager.ui.components.settings.*

class SettingsScreen : Screen {
    override val key = "Settings"

    @Composable
    override fun Content() {
        val model = getScreenModel<SettingsModel>()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.navigation_settings)) },
                    navigationIcon = { BackButton() },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(state = rememberScrollState())
            ) {
                val preferences = model.preferences

                if (model.showThemeDialog) {
                    ThemeDialog(
                        currentTheme = preferences.theme,
                        onDismissRequest = model::hideThemeDialog,
                        onConfirm = model::setTheme
                    )
                }

                SettingsHeader(stringResource(R.string.settings_appearance))

                SettingsItem(
                    modifier = Modifier.clickable(onClick = model::showThemeDialog),
                    icon = { Icon(painterResource(R.drawable.ic_brush), null) },
                    text = { Text(stringResource(R.string.settings_theme)) }
                ) {
                    FilledTonalButton(onClick = model::showThemeDialog) {
                        Text(preferences.theme.toDisplayName())
                    }
                }

                SettingsSwitch(
                    label = stringResource(R.string.setting_dynamic_color),
                    pref = preferences.dynamicColor,
                    icon = { Icon(painterResource(R.drawable.ic_palette), null) }
                ) {
                    preferences.dynamicColor = it
                }

                SettingsHeader(stringResource(R.string.settings_advanced))

                // SettingsTextField(
                //     label = stringResource(R.string.setting_app_name),
                //     pref = preferences.appName,
                //     onPrefChange = model::setAppName
                // )
                Spacer(modifier = Modifier.height(4.dp))

                // SettingsSwitch(
                //     label = stringResource(R.string.setting_replace_icon),
                //     secondaryLabel = stringResource(R.string.setting_replace_icon_desc),
                //     pref = preferences.replaceIcon,
                //     icon = { Icon(painterResource(R.drawable.ic_app_shortcut), null) }
                // ) {
                //     preferences.replaceIcon = it
                // }

                SettingsSwitch(
                    label = stringResource(R.string.setting_keep_patched_apks),
                    secondaryLabel = stringResource(R.string.setting_keep_patched_apks_desc),
                    icon = { Icon(painterResource(R.drawable.ic_delete_forever), null) },
                    pref = preferences.keepPatchedApks,
                    onPrefChange = { preferences.keepPatchedApks = it },
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSwitch(
                    label = stringResource(R.string.settings_developer_options),
                    pref = preferences.devMode,
                    icon = { Icon(painterResource(R.drawable.ic_code), null) }
                ) {
                    preferences.devMode = it
                }

                AnimatedVisibility(
                    visible = preferences.devMode,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // SettingsSwitch(
                        //     label = stringResource(R.string.setting_debuggable),
                        //     secondaryLabel = stringResource(R.string.setting_debuggable_desc),
                        //     pref = preferences.debuggable,
                        //     icon = { Icon(painterResource(R.drawable.ic_bug), null) },
                        //     onPrefChange = { preferences.debuggable = it },
                        // )
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    shape = ShapeDefaults.Large,
                    onClick = model::clearCacheDir
                ) {
                    Text(
                        text = stringResource(R.string.setting_clear_cache),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeDialog(
    currentTheme: Theme,
    onDismissRequest: () -> Unit,
    onConfirm: (Theme) -> Unit,
) {
    var selectedTheme by rememberSaveable { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_brush),
                contentDescription = stringResource(R.string.settings_theme)
            )
        },
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                Theme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .clickable { selectedTheme = theme }
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = theme.toDisplayName(),
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
                Text(stringResource(R.string.action_apply))
            }
        }
    )
}
