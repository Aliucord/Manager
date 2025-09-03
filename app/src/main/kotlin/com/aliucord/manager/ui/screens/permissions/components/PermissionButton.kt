package com.aliucord.manager.ui.screens.permissions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun PermissionButton(
    name: String,
    description: String,
    granted: Boolean,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = modifier
            .heightIn(min = 64.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Text(name)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                )

                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    ),
                ) {
                    Text(
                        text = description,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedButton(
                    onClick = onClick,
                    enabled = !granted,
                ) {
                    Text(if (granted) "Granted" else "Grant")
                }
            }
        }
    }
}
