package com.aliucord.manager.ui.widgets.updater

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliucord.manager.R
import org.koin.androidx.compose.getViewModel

@Composable
fun UpdaterDialog(
    viewModel: UpdaterViewModel = getViewModel(),
) {
    if (!viewModel.showDialog) return

    AlertDialog(
        confirmButton = {
            Button(onClick = viewModel::triggerUpdate) {
                if (!viewModel.isWorking) {
                    Text(stringResource(R.string.action_update))
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier
                            .size(20.dp)
                    )
                }
            }
        },
        dismissButton = {
            Button(
                onClick = viewModel::dismissDialog,
                enabled = !viewModel.isWorking
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        },
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.updater_title, viewModel.targetVersion ?: ""))
        },
        text = { Text(stringResource(R.string.updater_body)) },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
