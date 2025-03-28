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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.network.services.AliucordGithubService
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
        val model = getScreenModel<SettingsModel>()
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

                SettingsHeader(stringResource(R.string.settings_advanced))

                SettingsSwitch(
                    label = stringResource(R.string.settings_developer_options),
                    secondaryLabel = stringResource(R.string.settings_developer_options_desc),
                    pref = preferences.devMode,
                    icon = { Icon(painterResource(R.drawable.ic_code), null) },
                    onPrefChange = { preferences.devMode = it },
                )

                SettingsSwitch(
                    label = stringResource(R.string.setting_keep_patched_apks),
                    secondaryLabel = stringResource(R.string.setting_keep_patched_apks_desc),
                    icon = { Icon(painterResource(R.drawable.ic_delete_forever), null) },
                    pref = preferences.keepPatchedApks,
                    onPrefChange = model::setKeepPatchedApks,
                    modifier = Modifier.padding(bottom = 18.dp),
                )

                MainActionButton(
                    text = stringResource(R.string.setting_clear_cache),
                    icon = painterResource(R.drawable.ic_delete_forever),
                    enabled = !clearedCache,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    onClick = {
                        clearedCache = true
                        model.clearCacheDir()
                    },
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .fillMaxWidth()
                )

                if (preferences.keepPatchedApks) {
                    val handler = LocalUriHandler.current
                    MainActionButton(
                        text = stringResource(R.string.setting_see_patched_apks),
                        icon = painterResource(R.drawable.ic_launch),
                        onClick = { handler.openUri(AliucordGithubService.PATCHED_APKS_INFO_URL) },
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
