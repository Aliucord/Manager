package com.aliucord.manager.ui.components.dialogs

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.google.accompanist.permissions.*

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun StoragePermissionsDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        var manageStorageGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }

        if (!manageStorageGranted) {
            ManageStorageDialog(
                onGranted = { manageStorageGranted = true },
            )
        }
    } else {
        val writeStorageState = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (!writeStorageState.status.isGranted) {
            ExternalStorageDialog(onRequestPermission = writeStorageState::launchPermissionRequest)
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.R)
fun ManageStorageDialog(
    onGranted: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Environment.isExternalStorageManager()) {
                    onGranted()
                }
            }

            Button(
                onClick = {
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
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
                text = stringResource(R.string.storage_permissions_grant_body),
                textAlign = TextAlign.Center,
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun ExternalStorageDialog(
    onRequestPermission: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text(stringResource(android.R.string.ok))
            }
        },
        title = { Text(stringResource(R.string.permissions_grant_title)) },
        text = {
            Text(
                text = stringResource(R.string.storage_permissions_grant_body),
                textAlign = TextAlign.Center,
            )
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_tools),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
