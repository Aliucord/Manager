package com.aliucord.manager.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliucord.manager.preferences.Preference
import com.aliucord.manager.ui.components.ListItem
import com.aliucord.manager.ui.components.Switch

@Composable
fun PreferenceItem(
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    preference: Preference<Boolean>
) = ListItem(
    icon = icon,
    modifier = Modifier.clickable {
        preference.set(!preference.get())
    },
    text = title,
    secondaryText = description,
    trailing = {
        Switch(
            checked = preference.get(),
            onCheckedChange = preference::set
        )
    }
)