package com.aliucord.manager.ui.screens.permissions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionButton(
    name: String,
    description: String,
    granted: Boolean,
    required: Boolean,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .heightIn(min = 64.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )

            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Text(name)

                if (required) {
                    Text(
                        text = "＊",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
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
