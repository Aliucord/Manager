package com.aliucord.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadFailure(onRetry: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Failed to load",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(text = "Retry")
        }
    }
}
