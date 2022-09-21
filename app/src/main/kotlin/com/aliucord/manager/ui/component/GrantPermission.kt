/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GrantPermission(permissionState: PermissionState) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.permission_required),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Row {
                val context = LocalContext.current

                Button(onClick = permissionState::launchPermissionRequest) {
                    Text(stringResource(R.string.permissions_grant))
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                /* action = */ Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                /* uri = */ Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
    }
}
