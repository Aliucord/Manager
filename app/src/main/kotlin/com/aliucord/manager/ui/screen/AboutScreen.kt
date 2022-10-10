/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.contributors
import com.aliucord.manager.ui.viewmodel.AboutViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = getViewModel(),
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    AsyncImage(
                        model = "https://github.com/Aliucord.png",
                        contentDescription = "Aliucord",
                        modifier = Modifier.size(71.dp)
                    )

                    Text(
                        text = "Aliucord",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 26.sp
                        )
                    )

                    Text(
                        text = stringResource(R.string.aliucord_description),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                imageVector = Icons.Filled.Link,
                                contentDescription = stringResource(R.string.website)
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(text = stringResource(id = R.string.website))
                        }
                    }

                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    UserEntry("Vendicated", "the ven")
                    UserEntry("Juby210", "Fox")
                    UserEntry("Rushii", "explod", "diamondminer88")
                }
            }

            contributors(viewModel.contributors)
        }
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
