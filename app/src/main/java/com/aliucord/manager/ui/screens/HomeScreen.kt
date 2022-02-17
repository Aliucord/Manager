/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.Manifest
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.preferences.devModePreference
import com.aliucord.manager.ui.Screen
import com.aliucord.manager.ui.components.PluginsList
import com.aliucord.manager.ui.components.installer.InstallerDialog
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.Versions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalPermissionsApi
@ExperimentalFoundationApi
@Composable
fun HomeScreen(navController: NavController) {
    val storagePermissionState = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val packageManager = LocalContext.current.packageManager
    val versions = remember { mutableStateOf<Versions?>(null) }
    val installedVersion = remember {
        try {
            packageManager.getPackageInfo("com.aliucord", 0).versionName
        } catch (th: Throwable) {
            "-"
        }
    }

    val showOptionsDialog = remember { mutableStateOf(false) }

    if (versions.value == null) LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                versions.value = Github.versions
            } catch (th: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to get versions", th)
            }
        }
    }

    if (showOptionsDialog.value) InstallerDialog(showOptionsDialog, navController)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        "Aliucord",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(buildAnnotatedString {
                        append("Supported version: ")

                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(versions.value?.versionName ?: "?")
                        }

                        append("\nInstalled version: ")

                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(installedVersion)
                        }
                    })
                }

                val (drawable, description) = when {
                    installedVersion == "-" -> R.drawable.ic_download_24dp to R.string.install
                    installedVersion.startsWith(
                        versions.value?.versionName ?: "uwu"
                    ) -> R.drawable.ic_reinstall_24dp to R.string.reinstall
                    else -> R.drawable.ic_update_24dp to R.string.update
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//
//
//                        Button(
//                            onClick = {
//                                navController.navigate(Screen.Commits.route) {
//                                    popUpTo(Screen.Home.route) { saveState = true }
//                                }
//                            },
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
//                        ) {
//                            Icon(
//                                painter = painterResource(R.drawable.ic_update_24dp),
//                                contentDescription = "Commits",
//                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
//                                modifier = Modifier.padding(8.dp)
//                            )
//                        }
//                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (devModePreference.get()) {
                                showOptionsDialog.value = true
                            } else {
                                navController.navigate(Screen.Installer.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(drawable),
                            contentDescription = stringResource(description),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp)
                        )

                        Text(stringResource(description))
                    }

                    BoxWithConstraints {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
//                            if (installedVersion != "-") {
                            if (true) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    onClick = { }
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Uninstall",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }

                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Launch",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate(Screen.Commits.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    modifier = Modifier.padding(8.dp),
                                    painter = painterResource(R.drawable.ic_update_24dp),
                                    contentDescription = "Commits",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                if (this@BoxWithConstraints.minWidth > 20.dp) {
                                    Text("Commits", color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plugins",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.weight(1f, true))
                }

                PluginsList()
            }
        }
    }
}