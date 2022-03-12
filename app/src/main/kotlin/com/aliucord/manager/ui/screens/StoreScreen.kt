/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.annotation.Keep
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliucord.manager.utils.httpClient
import com.ramcosta.composedestinations.annotation.Destination
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Keep
private data class Tree(
    val tree: List<File>
) {
    @Keep
    data class File(
        val mode: String,
        val sha: String
    )
}

@Destination
@Composable
fun StoreScreen() {
    val plugins = remember { mutableStateListOf<Tree.File>() }

    Column {
        if (plugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.Center,
                content = { CircularProgressIndicator() }
            )

            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    val content =
                        httpClient.get<Tree>("https://api.github.com/repos/DiamondMiner88/AllACPlugins/git/trees/master") {
                            header("accept", "application/json")
                        }

                    plugins.addAll(content.tree.filter { it.mode == "160000" })
                }
            }
        } else {
            LazyColumn {
                items(plugins) { item ->
                    Text("SHA: ${item.sha}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
