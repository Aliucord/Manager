package com.aliucord.manager.ui.screens.iconopts.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aliucord.manager.ui.components.Label

@Composable
fun RadioSelectorItem(
    name: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier.Companion,
    onClick: () -> Unit,
) {
    val interactionSource = remember(::MutableInteractionSource)

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 12.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Companion.Switch,
            ),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            interactionSource = interactionSource,
        )

        Label(
            name = name,
            description = description,
            content = {},
        )
    }
}
