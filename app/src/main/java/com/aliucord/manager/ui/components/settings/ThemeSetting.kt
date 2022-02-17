package com.aliucord.manager.ui.components.settings

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.aliucord.manager.preferences.themePref
import com.aliucord.manager.ui.theme.Theme

@ExperimentalMaterialApi
@Composable
fun ThemeSetting(openDialog: MutableState<Boolean>) {
    ListItem(
        text = { Text(stringResource(R.string.theme)) },
        secondaryText = { Text(stringResource(R.string.theme_setting_description))},
        trailing = {
            FilledTonalButton(
                onClick = { openDialog.value = true }
            ) {
                Text(Theme.from(themePref.get()).displayName)
            }
        }
    )
}