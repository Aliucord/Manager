package com.aliucord.manager.ui.components.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            FilledTonalButton(onClick = context::launchPlayProtect,                ) {
                Text("Open GPP")
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
                Text("Continue")
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
        title = { Text("Play Protect") },
        text = {
            Text(
                text = "Google Play Protect appears to be enabled on your device. It may attempt to interfere with a new installation due to using a unique untrusted signing key.\n\nIn that case, press \"More Details\" -> \"Install anyway\"",
                textAlign = TextAlign.Center,
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
        ),
        modifier = modifier,
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
