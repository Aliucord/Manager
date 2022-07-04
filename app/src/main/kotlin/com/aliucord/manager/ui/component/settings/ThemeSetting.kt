/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSetting(
    onClick: () -> Unit,
) = ListItem(
    modifier = Modifier.clickable { onClick() },
    headlineText = { Text(stringResource(R.string.theme)) },
    supportingText = { Text(stringResource(R.string.theme_setting_description)) },
    trailingContent = {
        FilledTonalButton(
            onClick = onClick
        ) {
            Text(Theme.from(Prefs.theme.get()).displayName)
        }
    }
)
