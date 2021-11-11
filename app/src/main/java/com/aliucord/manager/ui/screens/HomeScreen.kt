/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.CommitsList
import com.aliucord.manager.ui.theme.getTheme
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.Versions
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun HomeScreen() {
    val packageManager = LocalContext.current.packageManager
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val selectedCommit = remember { mutableStateOf<String?>(null) }
    val versions = remember { mutableStateOf<Versions?>(null) }
    val installedVersion = remember {
        try {
            packageManager.getPackageInfo("com.aliucord", 0).versionName
        } catch (th: Throwable) { "-" }
    }

    if (versions.value == null) {
        LaunchedEffect(null) {
            coroutineScope.launch {
                try {
                    versions.value = Github.versions
                } catch (th: Throwable) {
                    Log.e(BuildConfig.TAG, "Failed to get versions")
                }
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Card(modifier = Modifier.padding(bottom = 8.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Aliucord", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.primary)
                    Text(buildAnnotatedString {
                        append("Supported version: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(
                                versions.value?.versionName ?: "?"
                            )
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
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        painter = painterResource(drawable),
                        contentDescription = stringResource(description),
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colors.primary, CircleShape)
                            .padding(7.dp)
                    )
                }
            }
        }
        Card { CommitsList(selectedCommit) }
    }

    rememberSystemUiController().setNavigationBarColor(
        color = getTheme().background
    )
}
