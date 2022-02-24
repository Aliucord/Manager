/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import com.aliucord.manager.R
import com.aliucord.manager.preferences.*
import com.aliucord.manager.ui.components.Switch
import com.aliucord.manager.ui.components.settings.SettingSwitch
import com.aliucord.manager.ui.components.settings.ThemeSetting
import com.aliucord.manager.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@Composable
fun SettingsScreen() = Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    val openDialog = remember { mutableStateOf(false) }

    if (openDialog.value) {
        var selected by remember { mutableStateOf(Theme.from(themePref.get())) }

        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text("Theme") },
            text = {
                Column {
                    Theme.values().forEach { theme ->
                        Row(
                            modifier = Modifier.clickable {
                                selected = theme
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                theme.displayName,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(Modifier.weight(1f, true))

                            RadioButton(
                                selected = theme == selected,
                                onClick = { selected = theme }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openDialog.value = false
                        themePref.set(selected.ordinal)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm"
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text("Apply")
                }
            }
        )
    }

    ThemeSetting(openDialog)

    ListItem(
        text = { Text(stringResource(R.string.use_black)) },
        secondaryText = { Text(stringResource(R.string.use_black_description)) },
        trailing = { SettingSwitch(preference = Preferences.useBlack) }
    )

    Divider()

    ListItem(
        text = { Text(stringResource(R.string.replace_bg)) },
        trailing = { SettingSwitch(preference = replaceBg) }
    )

    val devModeOn = devModePreference.get()
    ListItem(
        text = { Text(stringResource(R.string.developer_mode)) },
        trailing = {
            Switch(
                checked = devModeOn,
                onCheckedChange = devModePreference::set
            )
        }
    )

    if (devModeOn) {
        ListItem(
            text = { Text(stringResource(R.string.debuggable)) },
            trailing = { SettingSwitch(preference = debuggablePreference) }
        )

        val udfs = useDexFromStoragePreference

        ListItem(
            text = { Text(stringResource(R.string.use_dex_from_storage)) },
            trailing = { SettingSwitch(preference = udfs) }
        )

        val color = if (udfs.get()) {
            Color.Unspecified
        } else {
            MaterialTheme.colorScheme.onBackground.copy(ContentAlpha.disabled)
        }

        val test = remember { mutableStateOf("") }
        val result = remember { mutableStateOf<Uri?>(null) }
        val context = LocalContext.current
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            test.value = uri.toFile().absolutePath
        }

        ListItem(
            modifier = if (udfs.get()) Modifier.clickable {
                picker.launch(arrayOf("application/octet-stream"))
            } else Modifier,
            text = { Text(stringResource(R.string.dex_location), color = color) },
            secondaryText = { Text(dexLocationPreference.get(), color = color) }
        )
    }

    Button(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = { /*TODO*/ }
    ) {
        Text(stringResource(R.string.clear_files_cache), textAlign = TextAlign.Center)
    }
}