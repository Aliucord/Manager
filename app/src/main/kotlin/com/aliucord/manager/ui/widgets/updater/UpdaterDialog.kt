package com.aliucord.manager.ui.widgets.updater

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliucord.manager.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpdaterDialog(
    viewModel: UpdaterViewModel = koinViewModel(),
) {
    if (!viewModel.showDialog) return

    val isWorking by viewModel.isWorking.collectAsState(initial = false)

    AlertDialog(
        confirmButton = {
            FilledTonalButton(
                onClick = viewModel::triggerUpdate,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (!isWorking) {
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
            TextButton(
                onClick = viewModel::dismissDialog,
                enabled = !isWorking,
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        },
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.updater_title, viewModel.targetVersion ?: ""))
        },
        text = {
            Text(
                text = stringResource(R.string.updater_body),
                textAlign = TextAlign.Center,
            )
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
