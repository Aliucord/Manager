/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.network.dto.GithubUser

@Composable
fun ContributorsCard(contributors: List<GithubUser>) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.contributors),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (contributors.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false
                ) {
                    items(contributors) { user -> ContributorEntry(user.name) }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                    content = { CircularProgressIndicator() }
                )
            }
        }
    }
}

@Composable
fun ContributorEntry(name: String) {
    val uriHandler = LocalUriHandler.current

    AsyncImage(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .clickable { uriHandler.openUri("https://github.com/$name") },
        model = "https://github.com/$name.png",
        contentDescription = name
    )
}
