/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.aliucord.manager.R

@Composable
fun ContributorEntry(name: String) {
    val uriHandler = LocalUriHandler.current

    AsyncImage(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .clickable { uriHandler.openUri("https://github.com/$name") },
        model = "https://github.com/$name.png",
        contentDescription = stringResource(R.string.contributor)
    )
}
