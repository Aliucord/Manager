package com.aliucord.manager.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.aliucord.manager.ui.util.thenIf

@Composable
fun TextBanner(
    text: String,
    icon: Painter,
    iconColor: Color,
    outlineColor: Color?,
    containerColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .thenIf(outlineColor) { color ->
                border(
                    width = 2.dp,
                    color = color,
                    shape = MaterialTheme.shapes.medium,
                )
            }
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .thenIf(onClick) { clickable(onClick = it) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                painter = icon,
                tint = iconColor,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
