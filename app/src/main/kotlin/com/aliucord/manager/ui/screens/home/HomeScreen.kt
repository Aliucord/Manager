/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ProjectHeader
import com.aliucord.manager.ui.components.dialogs.InstallerDialog
import com.aliucord.manager.ui.screens.about.AboutScreen
import com.aliucord.manager.ui.screens.home.components.InfoCard
import com.aliucord.manager.ui.screens.home.components.InstalledItemCard
import com.aliucord.manager.ui.screens.install.InstallScreen
import com.aliucord.manager.ui.screens.plugins.PluginsScreen
import com.aliucord.manager.ui.screens.settings.SettingsScreen
import com.aliucord.manager.ui.util.DiscordVersion

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

        Scaffold(
            topBar = { HomeAppBar() },
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                ProjectHeader()

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                ) {

                AnimatedVisibility(
                    enter = fadeIn() + slideInVertically { it * -2 },
                    visible = model.installedVersion !is DiscordVersion.None,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.version_supported))
                            }
                            append(" ")
                            if (model.installedVersion is DiscordVersion.Existing) {
                                append((model.installedVersion as DiscordVersion.Existing).name)
                                append(" - ")
                            }
                            append(model.installedVersion.toDisplayName())
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = .5f),
                    )
                }

                FilledTonalIconButton(
                    shape = MaterialTheme.shapes.large,
                    enabled = true, // TODO: disable when installation is already present (no multi-install support yet)
                    onClick = { showInstallerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.action_add_install),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                val ctx = LocalContext.current
                val appIcon = remember {
                    ctx.packageManager
                        .getApplicationIcon("com.aliucord")
                        .toBitmap()
                        .asImageBitmap()
                        .let(::BitmapPainter)
                }

                InstalledItemCard(
                    appIcon = appIcon,
                    appName = "Aliucord",
                    packageName = "com.aliucord",
                    discordVersion = model.installedVersion,
                    onOpenApp = model::launchAliucord,
                    onOpenInfo = {},
                    onUninstall = model::uninstallAliucord,
                )

                InstalledItemCard(
                    appIcon = appIcon,
                    appName = "Aliucord test",
                    packageName = "com.aliucord2",
                    discordVersion = model.installedVersion,
                    onOpenApp = model::launchAliucord,
                    onOpenInfo = {},
                    onUninstall = model::uninstallAliucord,
                )
                    }

                Spacer(Modifier.height(120.dp))
                InfoCard(
                    packageName = model.preferences.packageName,
                    supportedVersion = model.supportedVersion,
                    currentVersion = model.installedVersion,
                    onDownloadClick = { showInstallerDialog = true },
                    onLaunchClick = model::launchAliucord,
                    onUninstallClick = model::uninstallAliucord
                )
            }
        }
    }
}

@Composable
private fun HomeAppBar() {
    TopAppBar(
        title = {},
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

            IconButton(onClick = { navigator.push(PluginsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_extension),
                    contentDescription = stringResource(R.string.navigation_about)
                )
            }

            IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings)
                )
            }
        }
    )
}
