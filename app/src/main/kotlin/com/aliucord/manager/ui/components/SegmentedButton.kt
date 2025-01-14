package com.aliucord.manager.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.SegmentedButton(
    icon: Painter,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    iconDescription: String? = null,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .weight(1f)
            .padding(12.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = iconDescription,
            tint = iconColor,
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
        )
    }
}
