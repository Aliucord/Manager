/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screens.home

import android.os.Parcelable
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.AnimatedVersionDisplay
import com.aliucord.manager.ui.components.ProjectHeader
import com.aliucord.manager.ui.screens.home.components.*
import com.aliucord.manager.ui.screens.patchopts.PatchOptionsScreen
import com.aliucord.manager.ui.screens.plugins.PluginsScreen
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.*

@Parcelize
class HomeScreen : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<HomeModel>()

        // Refresh installations list when the screen changes or activity resumes
        LifecycleResumeEffect(Unit) {
            model.fetchInstallations()

            onPauseOrDispose {}
        }

        Scaffold(
            topBar = { HomeAppBar() },
        ) { padding ->
            when (model.installations) {
                is InstallsState.Fetched -> PresentInstallsContent(
                    model = model,
                    padding = padding,
                    onClickInstall = { navigator.push(PatchOptionsScreen(supportedVersion = model.supportedVersion)) },
                )

                InstallsState.Fetching -> LoadingInstallsContent(padding = padding)

                InstallsState.None -> NoInstallsContent(
                    onClickInstall = { navigator.push(PatchOptionsScreen(supportedVersion = model.supportedVersion)) },
                    modifier = Modifier
                        .padding(padding.exclude(PaddingValuesSides.Bottom)),
                )

                InstallsState.Error -> {
                    // This is ugly asf but it should never happen
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(padding),
                    ) {
                        Text(
                            text = "Failed to fetch installations",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingInstallsContent(padding: PaddingValues) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        ProjectHeader()
    }
}

@Composable
fun PresentInstallsContent(
    model: HomeModel,
    padding: PaddingValues,
    onClickInstall: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = padding
            .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding.exclude(PaddingValuesSides.Bottom))
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
    ) {
        item(key = "PROJECT_HEADER") {
            ProjectHeader()
        }

        item(key = "ADD_INSTALL_BUTTON") {
            InstallButton(
                secondaryInstall = true,
                onClick = onClickInstall,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(50.dp)
                    .fillMaxWidth()
            )
        }

        item(key = "SUPPORTED_VERSION") {
            AnimatedVersionDisplay(
                version = model.supportedVersion,
                modifier = Modifier.padding(bottom = 30.dp),
            )
        }

        val installations = (model.installations as? InstallsState.Fetched)?.data
            ?: return@LazyColumn

        items(installations, key = { it.packageName }) {
            AnimatedVisibility(
                enter = fadeIn() + slideInHorizontally { it * -2 },
                exit = fadeOut() + slideOutHorizontally { it * 2 },
                visible = model.supportedVersion !is DiscordVersion.None,
            ) {
                val navigator = LocalNavigator.currentOrThrow

                InstalledItemCard(
                    data = it,
                    onUpdate = { model.updateAliucord(it, navigator) },
                    onOpenApp = { model.launchApp(it.packageName) },
                    onOpenInfo = { model.openAppInfo(it.packageName) },
                    onOpenPlugins = { navigator.push(PluginsScreen()) }, // TODO: install-specific plugins
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun NoInstallsContent(
    onClickInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ProjectHeader()

        InstallButton(
            secondaryInstall = false,
            onClick = onClickInstall,
            modifier = Modifier
                .padding(12.dp)
                .height(height = 50.dp)
                .fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(.7f)
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = """ /ᐠﹷ ‸ ﹷ ᐟ\ﾉ""",
                style = MaterialTheme.typography.labelLarge
                    .copy(fontSize = 38.sp),
            )

            Text(
                text = stringResource(R.string.installs_no_installs),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
