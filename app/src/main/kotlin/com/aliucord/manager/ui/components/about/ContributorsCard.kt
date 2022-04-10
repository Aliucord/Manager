/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.models.github.GithubUser
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContributorsCard() = ElevatedCard(
    modifier = Modifier.wrapContentHeight().fillMaxWidth()
) {
    val contributors = remember { mutableStateListOf<GithubUser>() }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.contributors),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (contributors.isNotEmpty()) {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(contributors) { user -> ContributorEntry(user.name) }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center,
                content = { CircularProgressIndicator() }
            )

            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    contributors.addAll(httpClient.get(Github.contributorsUrl).body<List<GithubUser>>().sortedByDescending { it.contributions })
                }
            }
        }
    }
}
