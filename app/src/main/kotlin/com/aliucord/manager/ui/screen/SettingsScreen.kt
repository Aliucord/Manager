/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.R
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.component.settings.*
import com.aliucord.manager.ui.viewmodel.SettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun SettingsScreen() = Column(
    modifier = Modifier.verticalScroll(state = rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    val viewModifier: SettingsViewModel = viewModel()

    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeDialog(
            onDismissRequest = { showThemeDialog = false },
            onConfirm = { Prefs.theme.set(it.ordinal) }
        )
    }

    ThemeSetting(onClick = { showThemeDialog = true })

    PreferenceItem(
        title = { Text(stringResource(R.string.use_black)) },
        description = { Text(stringResource(R.string.use_black_description)) },
        preference = Prefs.useBlack
    )

    Divider()

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        value = Prefs.appName.get(),
        onValueChange = Prefs.appName::set,
        label = { Text(stringResource(R.string.app_name_setting)) },
        placeholder = { Text("Aliucord") },
        singleLine = true
    )

    PreferenceItem(
        title = { Text(stringResource(R.string.replace_bg)) },
        preference = Prefs.replaceBg
    )

    val devModeOn = Prefs.devMode.get()

    PreferenceItem(
        title = { Text(stringResource(R.string.developer_mode)) },
        preference = Prefs.devMode
    )

    AnimatedVisibility(
        visible = devModeOn,
        exit = shrinkVertically()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                value = Prefs.packageName.get(),
                onValueChange = Prefs.packageName::set,
                label = { Text(stringResource(R.string.package_name)) },
                placeholder = { Text("com.aliucord") },
                singleLine = true
            )

            PreferenceItem(
                title = { Text(stringResource(R.string.debuggable)) },
                description = { Text(stringResource(R.string.debuggable_description)) },
                preference = Prefs.debuggable
            )
        }
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = viewModifier::clearCacheDir
    ) {
        Text(stringResource(R.string.clear_files_cache), textAlign = TextAlign.Center)
    }
}
