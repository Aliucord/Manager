/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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

fun LazyListScope.contributors(contributors: List<GithubUser>) {

    item {
        Text(
            text = stringResource(R.string.contributors),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 16.dp)
        )
    }

    if (contributors.isNotEmpty()) {
        items(contributors) { user ->
            ContributorEntry(user = user)
        }
    } else {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
                content = { CircularProgressIndicator() }
            )
        }
    }
}

@Composable
fun ContributorEntry(user: GithubUser) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .clickable {
                uriHandler.openUri("https://github.com/${user.name}")
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://github.com/${user.name}.png",
            contentDescription = user.name,
            Modifier
                .size(45.dp)
                .clip(CircleShape)
        )

        Column {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.contributions, user.contributions),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }

    }
}
