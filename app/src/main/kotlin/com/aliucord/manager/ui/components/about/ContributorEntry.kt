/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.aliucord.manager.R

@Composable
fun ContributorEntry(name: String) {
    val uriHandler = LocalUriHandler.current

    Image(
        modifier = Modifier
            .size(48.dp)
            .clickable { uriHandler.openUri("https://github.com/$name") },
        painter = rememberImagePainter(
            data = "https://github.com/$name.png",
            builder = {
                transformations(CircleCropTransformation())
            }
        ),
        contentDescription = stringResource(R.string.contributor)
    )
}
