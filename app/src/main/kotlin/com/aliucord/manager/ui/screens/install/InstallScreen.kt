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
import com.aliucord.manager.ui.components.back
import com.aliucord.manager.ui.components.dialogs.InstallAbortDialog
import com.aliucord.manager.ui.components.installer.InstallGroup
import com.aliucord.manager.ui.components.installer.InstallStatus
import com.aliucord.manager.ui.screens.install.InstallModel.InstallStepGroup
import kotlinx.collections.immutable.toImmutableList

class InstallScreen : Screen {
    override val key = "Install"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<InstallModel>()

        var expandedGroup by remember { mutableStateOf<InstallStepGroup?>(null) }

        if (model.returnToHome)
            navigator.back(null)

        LaunchedEffect(model.currentStep) {
            expandedGroup = model.currentStep?.group
        }

        // Exit warning dialog
        var showAbortWarning by remember { mutableStateOf(false) }
        if (showAbortWarning) {
            InstallAbortDialog(
                onDismiss = { showAbortWarning = false },
                onConfirm = {
                    navigator.back(currentActivity = null)
                    model.clearCache()
                },
            )
        } else {
            BackHandler {
                showAbortWarning = true
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.installer)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { showAbortWarning = true },
                        ) {
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
                val isCurrentlyProcessing by remember {
                    derivedStateOf {
                        model.steps[model.currentStep]?.status == InstallStatus.ONGOING
                    }
                }

                if (isCurrentlyProcessing) {
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
                    for (group in InstallStepGroup.entries) key(group) {
                        InstallGroup(
                            name = stringResource(group.nameResId),
                            isCurrent = expandedGroup == group,
                            onClick = { expandedGroup = group },
                            subSteps = model.getSteps(group).toImmutableList(),
                        )
                    }

                    if (model.isFinished && model.stacktrace.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = model::clearCache) {
                                Text(stringResource(R.string.setting_clear_cache))
                            }
                        }
                    }

                    if (model.stacktrace.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                text = model.stacktrace,
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = model::clearCache) {
                                Text(stringResource(R.string.setting_clear_cache))
                            }

                            Spacer(Modifier.weight(1f, true))

                            OutlinedButton(onClick = model::saveDebugToFile) {
                                Text(stringResource(R.string.installer_save_file))
                            }

                            FilledTonalButton(onClick = model::copyDebugToClipboard) {
                                Text(stringResource(R.string.action_copy))
                            }
                        }
                    }
                }
            }
        }
    }
}
