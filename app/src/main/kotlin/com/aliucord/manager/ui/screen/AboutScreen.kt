/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.ContributorsCard
import com.aliucord.manager.ui.viewmodel.AboutViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = getViewModel(),
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val uriHandler = LocalUriHandler.current

            ElevatedCard {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.team),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserEntry("Juby210", "Fox")
                        UserEntry("Vendicated", "the ven")
                    }
                }
            }

            ContributorsCard(viewModel.contributors)

            Spacer(Modifier.weight(1f, true))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { uriHandler.openUri("https://github.com/Aliucord/Aliucord") }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_github_white_24dp),
                    contentDescription = stringResource(R.string.github)
                )

                Spacer(Modifier.width(ButtonDefaults.IconSpacing))

                Text(stringResource(R.string.github))
            }
        }
    }
}

@Composable
private fun UserEntry(name: String, roles: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                model = "https://github.com/$name.png",
                contentDescription = name
            )

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(roles)
            }
        }
    }
}
