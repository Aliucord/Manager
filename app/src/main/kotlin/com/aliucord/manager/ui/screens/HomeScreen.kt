/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.aliucord.manager.R
import com.aliucord.manager.models.Version
import com.aliucord.manager.preferences.devModePreference
import com.aliucord.manager.ui.Screen
import com.aliucord.manager.ui.components.PluginsList
import com.aliucord.manager.ui.components.installer.InstallerDialog
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.gson
import com.aliucord.manager.utils.httpClient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalPermissionsApi
@ExperimentalFoundationApi
@Composable
fun HomeScreen(navController: NavController) {
    val packageManager = LocalContext.current.packageManager
    var supportedVersion by remember { mutableStateOf<String?>(null) }
    val installedVersion = remember {
        try {
            packageManager.getPackageInfo("com.aliucord", 0).versionName
        } catch (th: Throwable) {
            "-"
        }
    }

    val showOptionsDialog = remember { mutableStateOf(false) }

    if (supportedVersion == null) LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val version = gson.fromJson(httpClient.get<String>(Github.dataUrl), Version::class.java)

            supportedVersion = "${version.versionName} - " + when (version.versionCode[3].toString()) {
                "0" -> "Stable"
                "1" -> "Beta"
                "2" -> "Alpha"
                else -> throw NoWhenBranchMatchedException()
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
                            append(supportedVersion ?: "?")
                        }

                        append("\nInstalled version: ")

                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(installedVersion)
                        }
                    })
                }

                val (drawable, description) = when (installedVersion) {
                    "-" -> R.drawable.ic_download_24dp to R.string.install
                    supportedVersion -> R.drawable.ic_reinstall_24dp to R.string.reinstall
                    else -> R.drawable.ic_update_24dp to R.string.update
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            if (installedVersion != "-") {
                                val context = LocalContext.current

                                Button(
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    onClick = {
                                        val packageURI = Uri.parse("package:com.aliucord")
                                        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)

                                        context.startActivity(uninstallIntent)
                                    }
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
                                    onClick = {
                                        packageManager.getLaunchIntentForPackage("com.aliucord")?.let {
                                            context.startActivity(it)
                                        } ?: Toast.makeText(context, "Failed to launch Aliucord", Toast.LENGTH_LONG)
                                            .show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(8.dp),
                                        painter = painterResource(R.drawable.ic_launch_24dp),
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