package com.aliucord.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R

@Composable
fun LoadFailure(onRetry: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.network_load_fail),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(text = stringResource(R.string.action_retry))
        }
    }
}
