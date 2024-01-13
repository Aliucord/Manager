/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.dialogs.InstallerDialog
import com.aliucord.manager.ui.components.home.CommitList
import com.aliucord.manager.ui.components.home.InfoCard
import com.aliucord.manager.ui.screens.about.AboutScreen
import com.aliucord.manager.ui.screens.install.InstallScreen

class HomeScreen : Screen {
    override val key = "Home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<HomeModel>()

        var showInstallerDialog by remember { mutableStateOf(false) }

        LaunchedEffect(model.preferences.packageName) {
            model.fetchInstalledVersion()
        }

        if (showInstallerDialog) {
            InstallerDialog(
                onDismiss = { showInstallerDialog = false },
                onConfirm = { data ->
                    showInstallerDialog = false
                    navigator.push(InstallScreen(data))
                }
            )
        }

        // TODO: add a way to open plugins and settings

        Scaffold(
            topBar = { HomeAppBar() },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    packageName = model.preferences.packageName,
                    supportedVersion = model.supportedVersion,
                    currentVersion = model.installedVersion,
                    onDownloadClick = { showInstallerDialog = true },
                    onLaunchClick = model::launchAliucord,
                    onUninstallClick = model::uninstallAliucord
                )

                CommitList(
                    commits = model.commits,
                    onRetry = { model.fetchSupportedVersion() }
                )
            }
        }
    }
}

@Composable
private fun HomeAppBar() {
    TopAppBar(
        title = { Text(stringResource(R.string.navigation_home)) },
        actions = {
            val uriHandler = LocalUriHandler.current
            val navigator = LocalNavigator.currentOrThrow

            IconButton(
                onClick = {
                    uriHandler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}")
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_discord),
                    contentDescription = stringResource(R.string.support_server)
                )
            }

            IconButton(onClick = { navigator.push(AboutScreen()) }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.navigation_about)
                )
            }
        }
    )
}
