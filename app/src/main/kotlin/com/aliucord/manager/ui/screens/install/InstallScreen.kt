/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.install

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import com.aliucord.manager.ui.screens.install.components.InstallAppBar
import com.aliucord.manager.ui.screens.install.components.StepGroupCard
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude
import com.aliucord.manager.util.isIgnoringBatteryOptimizations
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.parameter.parametersOf

class InstallScreen(private val data: InstallOptions) : Screen {
    override val key = "Install"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val model = getScreenModel<InstallModel> { parametersOf(data) }

        val state = model.state.collectAsState()
        val showMinimizationWarning = remember { !context.isIgnoringBatteryOptimizations() }

        LaunchedEffect(state.value) {
            if (state.value is InstallScreenState.CloseScreen)
                navigator.back(currentActivity = null)
        }

        // Prevent screen from turning off while working
        Wakelock(active = state.value is InstallScreenState.Working)

        // Exit warning dialog (dismiss itself if install process state changes, esp. for Success)
        var showAbortWarning by remember(model.state.collectAsState()) { mutableStateOf(false) }

        // The currently expanded step group on this screen
        var expandedGroup by remember { mutableStateOf<StepGroup?>(StepGroup.Prepare) }

        // Only show exit warning if currently working
        val onTryExit: () -> Unit = remember {
            {
                if (state.value == InstallScreenState.Working) {
                    showAbortWarning = true
                } else {
                    navigator.back(currentActivity = null)
                }
            }
        }

        // Close all groups when successfully finished everything
        LaunchedEffect(state.value) {
            if (state.value == InstallScreenState.Success)
                expandedGroup = null
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

        Scaffold(
            topBar = { InstallAppBar(onTryExit) },
            modifier = Modifier
                .clickable(
                    indication = null,
                    onClick = model::cancelAutoclose,
                    interactionSource = remember(::MutableInteractionSource),
                ),
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .padding(paddingValues.exclude(PaddingValuesSides.Bottom)),
            ) {
                if (state.value == InstallScreenState.Working) {
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
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = paddingValues.exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize(),
                ) {
                    if (showMinimizationWarning && !state.value.isFinished) {
                        item(key = "MINIMIZATION_WARNING_BANNER") {
                            Column {
                                TextBanner(
                                    text = stringResource(R.string.installer_minimization_warning),
                                    icon = painterResource(R.drawable.ic_warning),
                                    iconColor = MaterialTheme.customColors.onWarningContainer,
                                    outlineColor = MaterialTheme.customColors.warning,
                                    containerColor = MaterialTheme.customColors.warningContainer,
                                    modifier = Modifier
                                        .padding(bottom = 20.dp)
                                        .fillMaxWidth()
                                        .animateItemPlacement(),
                                )

                                HorizontalDivider(
                                    thickness = (.5).dp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    if (state.value is InstallScreenState.Failed) {
                        stickyHeader(key = "FAILED_BANNER") {
                            val handler = LocalUriHandler.current

                            Surface(Modifier.animateItemPlacement()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    TextBanner(
                                        text = "Installation failed! You can either retry or click this to open the Aliucord server for help.",
                                        icon = painterResource(R.drawable.ic_warning),
                                        iconColor = MaterialTheme.colorScheme.error,
                                        // outlineColor = MaterialTheme.colorScheme.error.copy(alpha = .5f),
                                        outlineColor = MaterialTheme.colorScheme.errorContainer,
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { handler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}") }
                                    )

                                    FilledTonalIconButton(
                                        shape = MaterialTheme.shapes.medium,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        onClick = model::restart,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_refresh),
                                                contentDescription = null,
                                            )
                                            Text(
                                                text = "Retry installation",
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        thickness = (.5).dp,
                                        modifier = Modifier
                                            .padding(top = 8.dp, bottom = 10.dp)
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    for ((group, steps) in model.installSteps?.entries ?: persistentListOf()) {
                        item(key = System.identityHashCode(group)) {
                            StepGroupCard(
                                name = stringResource(group.localizedName),
                                subSteps = steps,
                                isExpanded = expandedGroup == group,
                                onExpand = {
                                    model.cancelAutoclose()
                                    expandedGroup = group
                                },
                            )
                        }
                    }

                    // if (state.value is InstallScreenState.Success) {
                    //     item(key = "BUTTON_ROW") {
                    //         Row(
                    //             horizontalArrangement = Arrangement.End,
                    //             modifier = Modifier.fillMaxWidth()
                    //         ) {
                    //             FilledTonalButton(onClick = model::clearCache) {
                    //                 Text(stringResource(R.string.setting_clear_cache))
                    //             }
                    //         }
                    //     }
                    // }

                    if (state.value is InstallScreenState.Failed) {
                        val failureLog = (state.value as InstallScreenState.Failed).failureLog

                        item(key = "BUTTON_ROW") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(onClick = model::clearCache) {
                                    Text(stringResource(R.string.setting_clear_cache))
                                }

                                Spacer(Modifier.weight(1f, true))

                                OutlinedButton(onClick = model::saveFailureLog) {
                                    Text(stringResource(R.string.installer_save_file))
                                }

                                FilledTonalButton(onClick = model::copyDebugToClipboard) {
                                    Text(stringResource(R.string.action_copy))
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
                        //                 .padding(top = 20.dp)
                        //                 .clip(RoundedCornerShape(10.dp))
                        //                 .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
                        //                 .padding(24.dp)
                        //                 .horizontalScroll(rememberScrollState())
                        //         )
                        //     }
                        // }
                    }
                }
            }
        }
    }
}
