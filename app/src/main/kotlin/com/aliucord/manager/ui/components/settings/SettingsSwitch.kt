package com.aliucord.manager.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsSwitch(
    label: String,
    secondaryLabel: String? = null,
    disabled: Boolean = false,
    icon: @Composable () -> Unit = {},
    pref: Boolean,
    onPrefChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        modifier = modifier.clickable(enabled = !disabled) { onPrefChange(!pref) },
        text = { Text(text = label, softWrap = true) },
        icon = icon,
        secondaryText = {
            secondaryLabel?.let {
                Text(text = it)
            }
        }
    ) {
        Switch(
            checked = pref,
            enabled = !disabled,
            onCheckedChange = { onPrefChange(!pref) }
        )
    }
}
