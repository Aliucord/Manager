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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.*

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
                modifier = Modifier.padding(paddingValues),
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
                                .padding(start = 16.dp, bottom = 16.dp)
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
private fun ProjectHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        AsyncImage(
            model = "https://github.com/Aliucord.png",
            contentDescription = stringResource(R.string.aliucord),
            modifier = Modifier.size(71.dp)
        )

        Text(
            text = stringResource(R.string.aliucord),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 26.sp
            )
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val uriHandler = LocalUriHandler.current

            TextButton(onClick = { uriHandler.openUri("https://github.com/Aliucord") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_github_white_24dp),
                    contentDescription = stringResource(R.string.github)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.github))
            }

            TextButton(onClick = { uriHandler.openUri("https://aliucord.com") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = stringResource(R.string.website)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.website))
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
        UserEntry("rushii", "explod", "DiamondMiner88")
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
                indication = rememberRipple(bounded = false, radius = 90.dp),
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
