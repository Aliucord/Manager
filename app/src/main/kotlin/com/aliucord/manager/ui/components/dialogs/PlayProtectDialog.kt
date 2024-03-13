package com.aliucord.manager.ui.components.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.customColors

@Composable
fun PlayProtectDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            FilledTonalButton(onClick = context::launchPlayProtect) {
                Text(stringResource(R.string.play_protect_warning_open_gpp))
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
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
            Text(
                text = stringResource(R.string.play_protect_warning_desc),
                textAlign = TextAlign.Center,
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false,
        ),
        modifier = modifier
            .padding(25.dp),
    )
}

private fun Context.launchPlayProtect() {
    Intent("com.google.android.gms.settings.VERIFY_APPS_SETTINGS")
        .setPackage("com.google.android.gms")
        .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .also(::startActivity)
}
