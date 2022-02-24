package com.aliucord.manager.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ListItem(
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text("Title", style = MaterialTheme.typography.labelMedium)
            Text("Subtext", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.weight(1f, true))

        trailing()
    }
}