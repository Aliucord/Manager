package com.aliucord.manager.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.models.Commit
import com.aliucord.manager.utils.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CommitData(
    val commit: Commit,
    val buildSha: String?
)

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalMaterialApi
@Composable
fun CommitsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    val commits = remember { mutableStateListOf<CommitData>() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.Center,
                content = { CircularProgressIndicator() }
            )

            LaunchedEffect(null) {
                launch(Dispatchers.IO) {
                    try {
                        val buildCommits = Github.getCommits(
                            mapOf(
                                "sha" to "builds",
                                "path" to "Aliucord.dex",
                                "per_page" to "50"
                            )
                        )
                        val commits2 = Github.getCommits(mapOf("per_page" to "50"))
                        commits.addAll(commits2.map { c ->
                            CommitData(
                                c,
                                buildCommits.find { bc -> bc.commit.message.substring(6) == c.sha }?.sha
                            )
                        })
                        isLoading = false
                    } catch (e: Throwable) {
                        Log.e(BuildConfig.TAG, "Failed to get commits", e)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(commits.toList()) { data ->
                    val commit = data.commit
                    val localUriHandler = LocalUriHandler.current

                    ListItem(
                        modifier = Modifier.clickable {
                            localUriHandler.openUri(commit.htmlUrl)
                        },
                        text = {
                            Text(
                                commit.sha.substring(0, 7),
                                color = if (data.buildSha != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        },
                        secondaryText = { Text("${commit.commit.message.split("\n").first()} - ${commit.author.name}") },
                    )
                }
            }
        }
    }
}