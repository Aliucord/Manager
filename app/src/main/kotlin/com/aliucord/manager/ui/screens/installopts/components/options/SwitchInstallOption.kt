package com.aliucord.manager.ui.screens.installopts.components.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SwitchInstallOption(
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .alpha(.7f)
            )
        }

        Switch(
            checked = value,
            enabled = enabled,
            onCheckedChange = onValueChange,
            interactionSource = interactionSource,
        )
    }
}
