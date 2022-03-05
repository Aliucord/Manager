package com.aliucord.manager.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.components.ListItem
import com.aliucord.manager.ui.theme.Theme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThemeSetting(
    onClick: () -> Unit,
) = ListItem(
    modifier = Modifier.clickable { onClick() },
    text = { Text(stringResource(R.string.theme)) },
    secondaryText = { Text(stringResource(R.string.theme_setting_description)) },
    trailing = {
        FilledTonalButton(
            onClick = onClick
        ) {
            Text(Theme.from(Prefs.theme.get()).displayName)
        }
    }
)