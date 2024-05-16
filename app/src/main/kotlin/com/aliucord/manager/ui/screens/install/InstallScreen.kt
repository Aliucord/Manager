/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.install

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.ui.TextBanner
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.InstallerAbortDialog
import com.aliucord.manager.ui.components.dialogs.PlayProtectDialog
import com.aliucord.manager.ui.screens.install.components.*
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.util.paddings.*
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import com.aliucord.manager.util.isIgnoringBatteryOptimizations
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.filter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

private val VERTICAL_PADDING: Dp = 18.dp

@Parcelize
class InstallScreen(private val data: InstallOptions) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Install"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val model = getScreenModel<InstallModel> { parametersOf(data) }

        val state by model.state.collectAsState()
        val listState = rememberLazyListState()
        val showMinimizationWarning = remember { !context.isIgnoringBatteryOptimizations() }

        LaunchedEffect(state) {
            if (state is InstallScreenState.CloseScreen)
                navigator.back(currentActivity = null)
        }

        // Prevent screen from turning off while working
        Wakelock(active = state is InstallScreenState.Working)

        // Exit warning dialog (dismiss itself if install process state changes, esp. for Success)
        var showAbortWarning by remember(model.state.collectAsState()) { mutableStateOf(false) }

        // The currently expanded step group on this screen
        var expandedGroup by remember { mutableStateOf<StepGroup?>(StepGroup.Prepare) }

        // Only show exit warning if currently working
        val onTryExit: () -> Unit = remember {
            {
                if (state == InstallScreenState.Working) {
                    showAbortWarning = true
                } else {
                    navigator.back(currentActivity = null)
                }
            }
        }

        // Close all groups when successfully finished everything
        LaunchedEffect(state) {
            if (state == InstallScreenState.Success)
                expandedGroup = null

            listState.animateScrollToItem(0)
        }

        if (showAbortWarning) {
            InstallerAbortDialog(
                onDismiss = { showAbortWarning = false },
                onConfirm = {
                    navigator.back(currentActivity = null)
                    model.clearCache()
                },
            )
        } else {
            BackHandler(onBack = onTryExit)
        }

        if (model.showGppWarning) {
            PlayProtectDialog(onDismiss = model::dismissGPPWarning)
        }

        Scaffold(
            topBar = { InstallAppBar(onTryExit) },
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(VERTICAL_PADDING),
                modifier = Modifier
                    .padding(paddingValues.exclude(PaddingValuesSides.Bottom)),
            ) {
                if (state == InstallScreenState.Working) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(4.dp)
                            .fillMaxWidth()
                    )
                } else {
                    HorizontalDivider(
                        thickness = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedByLastAtBottom(0.dp),
                    contentPadding = paddingValues
                        .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top)
                        .add(PaddingValues(bottom = 25.dp)),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize(),
                ) {
                    item(key = "MINIMIZATION_WARNING") {
                        BannerSection(visible = showMinimizationWarning && !state.isFinished) {
                            TextBanner(
                                text = stringResource(R.string.installer_minimization_warning),
                                icon = painterResource(R.drawable.ic_warning),
                                iconColor = MaterialTheme.customColors.onWarningContainer,
                                outlineColor = MaterialTheme.customColors.warning,
                                containerColor = MaterialTheme.customColors.warningContainer,
                                modifier = Modifier
                                    .padding(bottom = VERTICAL_PADDING)
                                    .fillMaxWidth(),
                            )
                        }
                    }

                    item(key = "FAILED_BANNER") {
                        BannerSection(visible = state is InstallScreenState.Failed) {
                            val handler = LocalUriHandler.current

                            TextBanner(
                                text = "Installation failed! You can either retry or click this banner to open the Aliucord server for help.",
                                icon = painterResource(R.drawable.ic_warning),
                                iconColor = MaterialTheme.colorScheme.error,
                                outlineColor = null,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                onClick = { handler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}") },
                                modifier = Modifier
                                    .padding(bottom = VERTICAL_PADDING)
                                    .fillMaxWidth(),
                            )
                        }
                    }

                    for ((group, steps) in model.installSteps?.entries ?: persistentListOf()) {
                        item(key = System.identityHashCode(group)) {
                            StepGroupCard(
                                name = stringResource(group.localizedName),
                                subSteps = steps,
                                isExpanded = expandedGroup == group,
                                onExpand = { expandedGroup = group },
                                modifier = Modifier
                                    .padding(bottom = VERTICAL_PADDING)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    item(key = "BUTTONS") {
                        val filteredState by remember { model.state.filter { it.isNewlyFinished } }
                            .collectAsState(initial = null)

                        AnimatedVisibility(
                            visible = filteredState != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = ExitTransition.None,
                        ) {
                            Column {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .padding(bottom = VERTICAL_PADDING)
                                )

                                when (filteredState) {
                                    InstallScreenState.Success -> {
                                        MainActionButton(
                                            text = "Launch Aliucord",
                                            icon = painterResource(R.drawable.ic_launch),
                                            onClick = model::launchApp,
                                        )
                                    }

                                    is InstallScreenState.Failed -> {
                                        MainActionButton(
                                            text = "Retry installation",
                                            icon = painterResource(R.drawable.ic_refresh),
                                            onClick = model::restart,
                                        )
                                    }

                                    else -> error("unreachable")
                                }
                            }
                        }
                    }

                    item(key = "FUN_FACT") {
                        val facts = arrayOf(
                            "Did you know that Google Play Protect is useless?",
                            "Did you know Discord (allegedly) fired the Android team when introducing RNA?",
                            "Did you know Aliucord with 150+ plugins still somehow runs better than Discord's new app?",
                            "i am in your walls",
                        )

                        Text(
                            text = "FUN FACT: " + remember { facts.random() },
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = VERTICAL_PADDING)
                                .padding(bottom = 25.dp)
                                .fillMaxWidth()
                                .alpha(.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerSection(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
            .padding(bottom = VERTICAL_PADDING),
    ) {
        Column {
            content()

            HorizontalDivider(
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

// item(key = "ERROR_LOG") {
//     SelectionContainer {
//         Text(
//             text = failureLog,
//             style = MaterialTheme.typography.labelSmall,
//             fontFamily = FontFamily.Monospace,
//             softWrap = false,
//             modifier = Modifier
//                 .padding(top = VERTICAL_PADDING)
//                 .clip(RoundedCornerShape(10.dp))
//                 .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
//                 .padding(24.dp)
//                 .horizontalScroll(rememberScrollState())
//         )
//     }
// }
