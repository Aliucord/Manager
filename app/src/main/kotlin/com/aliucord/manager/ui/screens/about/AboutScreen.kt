/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil3.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude

class AboutScreen : Screen {
    override val key = "About"

    @Composable
    override fun Content() {
        val model = getScreenModel<AboutModel>()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.navigation_about)) },
                    navigationIcon = { BackButton() },
                )
            }
        ) { paddingValues ->
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paddingValues
                    .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top),
                modifier = Modifier
                    .padding(paddingValues.exclude(PaddingValuesSides.Bottom))
                    .padding(horizontal = 16.dp),
            ) {
                item(key = "PROJECT_HEADER") {
                    ProjectHeader()
                }

                if (!model.fetchError) {
                    item(key = "MAIN_CONTRIBUTORS") {
                        MainContributors()
                    }

                    item("CONTRIBUTORS_HEADER") {
                        Text(
                            text = stringResource(R.string.contributors),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }

                    if (model.contributors.isNotEmpty()) {
                        items(model.contributors, key = { "user-${it.name}" }) { user ->
                            ContributorCommitsItem(user)
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
                } else {
                    item(key = "LOAD_FAILURE") {
                        Box(
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            LoadFailure(onRetry = { model.fetchContributors() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContributors(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 20.dp)
    ) {
        UserEntry("Vendicated", "the ven")
        UserEntry("Juby210", "Fox")
        UserEntry("rushii", "explod", "rushiiMachine")
    }
}

@Composable
private fun UserEntry(name: String, roles: String, username: String = name) {
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .clickable(
                onClick = { uriHandler.openUri("https://github.com/$username") },
                indication = ripple(bounded = false, radius = 90.dp),
                interactionSource = remember { MutableInteractionSource() }
            )
            .widthIn(min = 100.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(71.dp)
                .clip(CircleShape),
            model = "https://github.com/$username.png",
            contentDescription = username
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                )
            )

            Text(
                text = roles,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}
