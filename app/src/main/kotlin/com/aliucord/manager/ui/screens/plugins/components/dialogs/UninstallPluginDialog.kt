package com.aliucord.manager.ui.screens.plugins.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R

@Composable
fun UninstallPluginDialog(
    pluginName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_forever),
                contentDescription = null,
            )
        },
        title = {
            Text(stringResource(R.string.plugins_delete_plugin, pluginName))
        },
        text = {
            Text(stringResource(R.string.plugins_delete_plugin_body))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
