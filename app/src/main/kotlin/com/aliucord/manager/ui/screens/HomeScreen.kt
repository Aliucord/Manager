/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import com.aliucord.manager.R
import com.aliucord.manager.models.github.Version
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.components.PluginsList
import com.aliucord.manager.ui.components.installer.DownloadMethod
import com.aliucord.manager.ui.components.installer.InstallerDialog
import com.aliucord.manager.ui.screens.destinations.CommitsScreenDestination
import com.aliucord.manager.ui.screens.destinations.InstallerScreenDestination
import com.aliucord.manager.utils.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val packageManager = LocalContext.current.packageManager
    var supportedVersion by remember { mutableStateOf<String?>(null) }
    val installedVersion = remember {
        try {
            packageManager.getPackageInfo(Prefs.packageName.get(), 0).versionName
        } catch (th: Throwable) {
            "-"
        }
    }

    LaunchedEffect(Unit) {
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

    var showOptionsDialog by remember { mutableStateOf(false) }

    if (showOptionsDialog) {
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            navigator.navigate(InstallerScreenDestination(uri.toFile()))
        }

        InstallerDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = { method ->
                if (method == DownloadMethod.DOWNLOAD) {
                    navigator.navigate(InstallerScreenDestination())
                } else {
                    filePicker.launch(arrayOf("application/octet-stream"))
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        "Aliucord${Prefs.packageName.get().let { if (it != "com.aliucord") " ($it)" else ""}}",
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f, true),
                            onClick = {
                                if (Prefs.devMode.get()) {
                                    showOptionsDialog = true
                                } else {
                                    navigator.navigate(InstallerScreenDestination())
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                painter = painterResource(drawable),
                                contentDescription = stringResource(description),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )

                            Text(stringResource(description))
                        }

                        if (installedVersion == "-") Button(
                            modifier = Modifier.wrapContentSize(),
                            onClick = { navigator.navigate(CommitsScreenDestination) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                painter = painterResource(R.drawable.ic_update_24dp),
                                contentDescription = "Commits",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (installedVersion != "-") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val context = LocalContext.current

                            Button(
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                onClick = {
                                    val packageURI = Uri.parse("package:${Prefs.packageName.get()}")
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
                                    packageManager.getLaunchIntentForPackage(Prefs.packageName.get())?.let {
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

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { navigator.navigate(CommitsScreenDestination) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    modifier = Modifier.padding(8.dp),
                                    painter = painterResource(R.drawable.ic_update_24dp),
                                    contentDescription = "Commits",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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
