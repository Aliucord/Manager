package com.aliucord.manager.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.SegmentedButton(
    icon: Painter,
    iconDescription: String? = null,
    text: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + 2.dp))
            .weight(1f)
            .padding(12.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    }
}
