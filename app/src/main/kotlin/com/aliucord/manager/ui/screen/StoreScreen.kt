/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.util.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class Tree(
    val tree: List<File>
) {
    @Serializable
    data class File(
        val mode: String,
        val sha: String
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onClickBack: () -> Unit) {
    val plugins = remember { mutableStateListOf<Tree.File>() }
    // TODO: Display cards with option to install plugin
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.store)) },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            if (plugins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                    content = { CircularProgressIndicator() }
                )

                LaunchedEffect(Unit) {
                    launch(Dispatchers.IO) {
                        val content = httpClient.get("https://api.github.com/repos/DiamondMiner88/AllACPlugins/git/trees/master") {
                            header("accept", "application/json")
                        }.body<Tree>()

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
}
