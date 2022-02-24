package com.aliucord.manager.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliucord.manager.preferences.Preference
import com.aliucord.manager.ui.components.Switch

@Composable
fun SettingSwitch(
    modifier: Modifier = Modifier,
    preference: Preference<Boolean>
) = Switch(
    modifier = modifier,
    checked = preference.get(),
    onCheckedChange = preference::set
)