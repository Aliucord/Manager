package com.aliucord.manager.ui.screens.patchopts.components.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SwitchPatchOption(
    icon: Painter,
    name: String,
    description: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember(::MutableInteractionSource)
    val onClick = remember(value) { { onValueChange(!value) } }

    IconPatchOption(
        icon = icon,
        name = name,
        description = description,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
            ),
    ) {
        Switch(
            checked = value,
            enabled = enabled,
            onCheckedChange = onValueChange,
            interactionSource = interactionSource,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
