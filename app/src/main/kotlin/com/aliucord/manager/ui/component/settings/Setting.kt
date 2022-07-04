/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliucord.manager.preferences.Preference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceItem(
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    preference: Preference<Boolean>
) = ListItem(
    modifier = Modifier.clickable {
        preference.set(!preference.get())
    },
    leadingContent = icon,
    headlineText = title,
    supportingText = description,
    trailingContent = {
        Switch(
            checked = preference.get(),
            onCheckedChange = preference::set
        )
    }
)
