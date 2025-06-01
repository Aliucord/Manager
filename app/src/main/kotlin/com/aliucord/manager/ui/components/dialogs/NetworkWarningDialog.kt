package com.aliucord.manager.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliucord.manager.R

@Composable
fun NetworkWarningDialog(
    onConfirm: (neverShow: Boolean) -> Unit,
    onDismiss: (neverShow: Boolean) -> Unit,
) {
    val interactionSource = remember(::MutableInteractionSource)
    var neverShow by rememberSaveable { mutableStateOf(false) }
    val rememberedNeverShow by rememberUpdatedState(neverShow)

    AlertDialog(
        onDismissRequest = { onDismiss(rememberedNeverShow) },
        properties = DialogProperties(
            dismissOnClickOutside = false,
        ),
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(rememberedNeverShow) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss(rememberedNeverShow) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
            ) {
                Text(stringResource(R.string.navigation_back))
            }
        },
        title = { Text(stringResource(R.string.network_warning_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.network_warning_body),
                    textAlign = TextAlign.Center,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { neverShow = !rememberedNeverShow },
                        )
                        .padding(end = 16.dp)
                ) {
                    Checkbox(
                        checked = neverShow,
                        onCheckedChange = { neverShow = it },
                        interactionSource = interactionSource,
                    )

                    Text(stringResource(R.string.network_warning_disable))
                }
            }
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
        textContentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}
