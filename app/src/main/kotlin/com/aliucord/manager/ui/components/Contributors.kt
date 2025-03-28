/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.network.models.Contributor
import com.valentinilk.shimmer.shimmer

@Composable
fun ContributorCommitsItem(
    user: Contributor,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .clickable { uriHandler.openUri("https://github.com/${user.username}") }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        SubcomposeAsyncImage(
            model = user.avatarUrl,
            contentDescription = user.username,
            error = {
                Surface(
                    content = {},
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmer(),
                )
            },
            modifier = Modifier
                .padding(top = 6.dp)
                .size(45.dp)
                .clip(CircleShape)
        )

        Column {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.contributors_contributions, user.commits),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )

            Text(
                text = user.repositories.joinToString { it.name },
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
