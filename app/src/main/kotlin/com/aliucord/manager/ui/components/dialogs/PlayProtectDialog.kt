package com.aliucord.manager.ui.components.dialogs

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
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
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.theme.customColors

@Composable
fun PlayProtectDialog(
    onDismiss: (neverShow: Boolean) -> Unit,
) {
    val activity = LocalActivity.currentOrThrow
    val interactionSource = remember(::MutableInteractionSource)
    var neverShow by rememberSaveable { mutableStateOf(false) }
    val rememberedNeverShow by rememberUpdatedState(neverShow)

    AlertDialog(
        onDismissRequest = { onDismiss(rememberedNeverShow) },
        dismissButton = {
            FilledTonalButton(onClick = activity::launchPlayProtect) {
                Text(stringResource(R.string.play_protect_warning_open_gpp))
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onDismiss(rememberedNeverShow) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.play_protect_warning_ok))
            }
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_protect_warning),
                tint = MaterialTheme.customColors.warning,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        },
        title = { Text(stringResource(R.string.play_protect_warning_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.play_protect_warning_desc),
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

                    Text(stringResource(R.string.play_protect_warning_disable))
                }
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
        modifier = Modifier
            .padding(25.dp),
    )
}

private fun Activity.launchPlayProtect() {
    Intent("com.google.android.gms.settings.VERIFY_APPS_SETTINGS")
        .setPackage("com.google.android.gms")
        .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        .also(::startActivity)
}
