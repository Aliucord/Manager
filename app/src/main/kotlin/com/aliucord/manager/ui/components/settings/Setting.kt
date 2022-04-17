/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.aliucord.manager.preferences.Preference
import com.aliucord.manager.ui.components.ListItem

@Composable
fun PreferenceItem(
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    preference: Preference<Boolean>
) = ListItem(
    icon = icon,
    modifier = Modifier.clickable(role = Role.Switch) {
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
