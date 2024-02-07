/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.install

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.ui.components.back
import com.aliucord.manager.ui.components.dialogs.InstallerAbortDialog
import com.aliucord.manager.ui.screens.install.components.StepGroupCard

class InstallScreen : Screen {
    override val key = "Install"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<InstallModel>()
        val state = model.state.collectAsState()

        LaunchedEffect(model.state) {
            if (model.state.value is InstallScreenState.CloseScreen)
                navigator.back(currentActivity = null)
        }

        // Exit warning dialog (dismiss itself if install process state changes, esp. for Success)
        var showAbortWarning by remember(model.state.collectAsState()) { mutableStateOf(false) }

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
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.installer)) },
                    navigationIcon = {
                        IconButton(onClick = onTryExit) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.navigation_back),
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                if (state.value is InstallScreenState.Working) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(bottom = 4.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    var expandedGroup by remember { mutableStateOf<StepGroup?>(StepGroup.Prepare) }

                    // Close all groups when successfully finished everything
                    LaunchedEffect(state.value) {
                        if (state.value == InstallScreenState.Success)
                            expandedGroup = null
                    }

                    model.installSteps?.let { groupedSteps ->
                        for ((group, steps) in groupedSteps.entries) key(group) {
                            StepGroupCard(
                                name = stringResource(group.localizedName),
                                subSteps = steps,
                                isExpanded = expandedGroup == group,
                                onExpand = { expandedGroup = group },
                            )
                        }
                    }

                    if (state.value.isFinished) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = model::clearCache) {
                                Text(stringResource(R.string.setting_clear_cache))
                            }
                        }
                    }

                    if (state.value is InstallScreenState.Failed) {
                        val failureLog = (state.value as InstallScreenState.Failed).failureLog

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

                        SelectionContainer {
                            Text(
                                text = failureLog,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
                                    .padding(10.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}