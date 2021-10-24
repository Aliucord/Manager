/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.preferences.dexLocationPreference
import com.aliucord.manager.preferences.replaceBgPreference
import com.aliucord.manager.preferences.themePreference
import com.aliucord.manager.preferences.useDexFromStoragePreference
import com.aliucord.manager.ui.components.ExposedDropdownMenu

private val themes = arrayOf("System", "Light", "Dark")

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen() {
    Column {
        val modifier = Modifier.padding(top = 8.dp)

        ListItem(
            text = { Text(stringResource(R.string.theme)) },
            trailing = {
                ExposedDropdownMenu(
                    items = themes,
                    selectedItem = themePreference.get(),
                    selectItem = themePreference::set
                )
            },
            modifier = modifier
        )
        ListItem(
            text = { Text(stringResource(R.string.replace_bg)) },
            trailing = {
                Checkbox(
                    checked = replaceBgPreference.get(),
                    onCheckedChange = replaceBgPreference::set
                )
            },
            modifier = modifier.clickable { replaceBgPreference.set(!replaceBgPreference.get()) }
        )
        val udfs = useDexFromStoragePreference.get()
        ListItem(
            text = { Text(stringResource(R.string.use_dex_from_storage)) },
            trailing = {
                Checkbox(
                    checked = udfs,
                    onCheckedChange = useDexFromStoragePreference::set
                )
            },
            modifier = modifier.clickable { useDexFromStoragePreference.set(!useDexFromStoragePreference.get()) }
        )
        val color =
            if (udfs) Color.Unspecified else MaterialTheme.colors.onBackground.copy(ContentAlpha.disabled)
        ListItem(
            text = { Text(stringResource(R.string.dex_location), color = color) },
            secondaryText = { Text(dexLocationPreference.get(), color = color) },
            modifier = if (udfs) modifier.clickable { /*TODO*/ } else modifier
        )
        Button(
            onClick = { /*TODO*/ },
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(stringResource(R.string.clear_files_cache), textAlign = TextAlign.Center)
        }
    }
}
