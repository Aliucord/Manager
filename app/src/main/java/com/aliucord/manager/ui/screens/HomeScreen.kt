/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.ui.components.CommitsList
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val selectedCommit = remember { mutableStateOf<String?>(null) }
    val versions = remember { mutableStateOf<Versions?>(null) }

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
            ListItem(
                text = { Text("Aliucord") },
                secondaryText = {
                    Text(buildAnnotatedString {
                        append("Supported version: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(
                                versions.value?.versionName ?: "?"
                            )
                        }
                    })
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Card { CommitsList(selectedCommit) }
    }
}
