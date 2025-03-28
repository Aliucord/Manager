package com.aliucord.manager.ui.components.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliucord.manager.R
import com.aliucord.manager.network.utils.SemVer

@Composable
fun CustomComponentVersionPicker(
    componentTitle: String,
    versions: SnapshotStateList<SemVer>,
    onConfirm: (SemVer) -> Unit,
    onDelete: (SemVer) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedVersion by rememberSaveable { mutableStateOf<SemVer?>(null) }

    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onCancel,
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(selectedVersion!!) },
                enabled = selectedVersion != null,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        },
        title = { Text(stringResource(R.string.custom_component_picker_title)) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.custom_component_picker_desc, componentTitle),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    for (version in versions) key(version) {
                        val clickSource = remember(::MutableInteractionSource)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = clickSource,
                                    indication = LocalIndication.current,
                                    onClick = { selectedVersion = version },
                                ),
                        ) {
                            RadioButton(
                                selected = selectedVersion == version,
                                onClick = { selectedVersion = version },
                                interactionSource = clickSource,
                            )

                            Text(
                                text = "v$version",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            )

                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { onDelete(version) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_forever),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
