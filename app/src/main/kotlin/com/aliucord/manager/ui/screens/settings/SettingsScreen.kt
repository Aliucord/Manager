/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.settings

import android.os.Build
import android.os.Parcelable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.components.MainActionButton
import com.aliucord.manager.ui.components.settings.*
import com.aliucord.manager.ui.screens.settings.components.ThemeDialog
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class SettingsScreen : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Settings"

    @Composable
    override fun Content() {
        val model = koinScreenModel<SettingsModel>()
        var clearedCache by rememberSaveable { mutableStateOf(false) }

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

                SettingsHeader(stringResource(R.string.settings_header_appearance))

                SettingsItem(
                    modifier = Modifier.clickable(onClick = model::showThemeDialog),
                    icon = { Icon(painterResource(R.drawable.ic_brush), null) },
                    text = { Text(stringResource(R.string.setting_theme)) },
                    secondaryText = { Text(stringResource(R.string.setting_theme_desc)) }
                ) {
                    FilledTonalButton(onClick = model::showThemeDialog) {
                        Text(preferences.theme.toDisplayName())
                    }
                }

                // Material You theming on Android 12+
                if (Build.VERSION.SDK_INT >= 31) {
                    SettingsSwitch(
                        label = stringResource(R.string.setting_dynamic_color),
                        secondaryLabel = stringResource(R.string.setting_dynamic_color_desc),
                        pref = preferences.dynamicColor,
                        icon = { Icon(painterResource(R.drawable.ic_palette), null) },
                        onPrefChange = { preferences.dynamicColor = it },
                    )
                }

                SettingsHeader(stringResource(R.string.settings_header_advanced))

                SettingsSwitch(
                    label = stringResource(R.string.setting_developer_options),
                    secondaryLabel = stringResource(R.string.setting_developer_options_desc),
                    pref = preferences.devMode,
                    icon = { Icon(painterResource(R.drawable.ic_code), null) },
                    onPrefChange = { preferences.devMode = it },
                )

                SettingsSwitch(
                    label = stringResource(R.string.setting_keep_patched_apks),
                    secondaryLabel = stringResource(R.string.setting_keep_patched_apks_desc),
                    icon = { Icon(painterResource(R.drawable.ic_delete_forever), null) },
                    pref = preferences.keepPatchedApks,
                    onPrefChange = { model.setKeepPatchedApks(it) },
                    modifier = Modifier.padding(bottom = 18.dp),
                )

                if (preferences.keepPatchedApks) {
                    MainActionButton(
                        text = stringResource(R.string.log_action_export_apk),
                        icon = painterResource(R.drawable.ic_save),
                        enabled = model.patchedApkExists,
                        onClick = model::shareApk,
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                            .fillMaxWidth()
                    )
                }

                MainActionButton(
                    text = stringResource(R.string.settings_clear_cache),
                    icon = painterResource(R.drawable.ic_delete_forever),
                    enabled = !clearedCache,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    onClick = {
                        clearedCache = true
                        model.clearCache()
                    },
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .fillMaxWidth()
                )

                SettingsHeader(stringResource(R.string.settings_header_info))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 36.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = model.installInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        ),
                    )

                    Spacer(Modifier.weight(1f, fill = true))

                    IconButton(onClick = model::copyInstallInfo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = stringResource(R.string.action_copy),
                            modifier = Modifier
                                .size(28.dp)
                                .alpha(.8f),
                        )
                    }
                }
            }
        }
    }
}
