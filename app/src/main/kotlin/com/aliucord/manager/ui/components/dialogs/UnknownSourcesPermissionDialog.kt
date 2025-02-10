package com.aliucord.manager.ui.components.dialogs

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R

@Composable
fun UnknownSourcesPermissionDialog() {
    // Permission does not exist below API 26
    if (Build.VERSION.SDK_INT < 26) return

    val context = LocalContext.current
    var permissionGranted by remember(context) { mutableStateOf(context.packageManager.canRequestPackageInstalls()) }

    if (!permissionGranted) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    permissionGranted = context.packageManager.canRequestPackageInstalls()
                }

                Button(
                    onClick = {
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData("package:${BuildConfig.APPLICATION_ID}".toUri())
                            .let { launcher.launch(it) }
                    }
                ) {
                    Text(stringResource(R.string.action_open_settings))
                }
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_tools),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = { Text(stringResource(R.string.permissions_grant_title)) },
            text = {
                Text(
                    text = stringResource(R.string.install_permissions_grant_body),
                    textAlign = TextAlign.Center,
                )
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
