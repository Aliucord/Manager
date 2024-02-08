/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ProjectHeader
import com.aliucord.manager.ui.components.dialogs.NetworkWarningDialog
import com.aliucord.manager.ui.screens.home.components.*
import com.aliucord.manager.ui.screens.installopts.InstallOptionsScreen
import com.aliucord.manager.ui.screens.plugins.PluginsScreen
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude

class HomeScreen : Screen {
    override val key = "Home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<HomeModel>()

        // Refresh installations list when the screen changes or activity resumes
        LifecycleResumeEffect {
            model.fetchInstallations()

            onPauseOrDispose {}
        }

        var showNetworkWarningDialog by remember { mutableStateOf(false) }
        val onClickInstall: () -> Unit = remember {
            {
                if (model.isNetworkDangerous()) {
                    showNetworkWarningDialog = true
                } else {
                    navigator.push(InstallOptionsScreen())
                }
            }
        }

        if (showNetworkWarningDialog) {
            NetworkWarningDialog(
                onConfirm = {
                    showNetworkWarningDialog = false
                    navigator.push(InstallOptionsScreen())
                },
                onDismiss = {
                    showNetworkWarningDialog = false
                },
            )
        }

        Scaffold(
            topBar = { HomeAppBar() },
        ) { paddingValues ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
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

                item(key = "ADD_INSTALL_BUTTON") {
                    InstallButton(
                        onClick = onClickInstall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
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
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string.version_supported))
                                    append(" ")
                                }
                            },
                            modifier = Modifier
                                .alpha(.5f)
                                .padding(bottom = 22.dp),
                        )
                    }
                }

                val installations = (model.installations as? InstallsState.Fetched)?.data
                    ?: return@LazyColumn

                items(installations, key = { it.packageName }) {
                    AnimatedVisibility(
                        enter = fadeIn() + slideInHorizontally { it * -2 },
                        exit = fadeOut() + slideOutHorizontally { it * 2 },
                        visible = model.supportedVersion !is DiscordVersion.None,
                    ) {
                        InstalledItemCard(
                            data = it,
                            onUpdate = ::TODO, // TODO: prefilled install options screen
                            onOpenApp = { model.launchApp(it.packageName) },
                            onOpenInfo = { model.openAppInfo(it.packageName) },
                            onOpenPlugins = { navigator.push(PluginsScreen()) }, // TODO: install-specific plugins
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
