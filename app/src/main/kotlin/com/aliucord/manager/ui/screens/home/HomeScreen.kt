/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ProjectHeader
import com.aliucord.manager.ui.components.dialogs.InstallerDialog
import com.aliucord.manager.ui.screens.home.components.*
import com.aliucord.manager.ui.screens.install.InstallScreen
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude

class HomeScreen : Screen {
    override val key = "Home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<HomeModel>()

        var showInstallerDialog by remember { mutableStateOf(false) }

        if (showInstallerDialog) {
            InstallerDialog(
                onDismiss = { showInstallerDialog = false },
                onConfirm = { data ->
                    showInstallerDialog = false
                    navigator.push(InstallScreen(data))
                }
            )
        }

        Scaffold(
            topBar = { HomeAppBar() },
        ) { paddingValues ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paddingValues
                    .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues.exclude(PaddingValuesSides.Bottom))
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                item(key = "PROJECT_HEADER") {
                    ProjectHeader()
                }

                item(key = "SUPPORTED_VERSION") {
                    AnimatedVisibility(
                        enter = fadeIn() + slideInVertically { it * -2 },
                        exit = fadeOut() + slideOutVertically { it * -2 },
                        visible = model.supportedVersion !is DiscordVersion.None,
                    ) {
                        VersionDisplay(
                            version = model.supportedVersion,
                            prefix = {
                                append(stringResource(R.string.version_supported))
                                append(" ")
                            },
                            modifier = Modifier.alpha(.5f),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                item(key = "INSTALL_BUTTON") {
                    AnimatedVisibility(
                        visible = model.installations !is InstallsFetchState.Fetching,
                        enter = fadeIn(
                            animationSpec = tween(delayMillis = 700)
                        ) + slideInVertically(
                            animationSpec = tween(delayMillis = 700),
                            initialOffsetY = { it * 3 },
                        ),
                        exit = ExitTransition.None,
                    ) {
                        InstallButton(
                            onClick = { showInstallerDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(4.dp))
                    }
                }

                val installations = (model.installations as? InstallsFetchState.Fetched)?.data
                    ?: return@LazyColumn

                items(installations, key = { it.packageName }) {
                    AnimatedVisibility(
                        enter = fadeIn() + slideInHorizontally { it * -2 },
                        exit = fadeOut() + slideOutHorizontally { it * 2 },
                        visible = model.supportedVersion !is DiscordVersion.None,
                    ) {
                        InstalledItemCard(
                            appIcon = it.icon,
                            appName = it.name,
                            packageName = it.packageName,
                            discordVersion = it.version,
                            onOpenApp = { }, // TODO: multi-install open app handler
                            onOpenInfo = {}, // TODO: multi-install open info handler
                            onUninstall = {}, // TODO: multi-install uninstall handler
                        )
                    }
                }
            }
        }
    }
}
