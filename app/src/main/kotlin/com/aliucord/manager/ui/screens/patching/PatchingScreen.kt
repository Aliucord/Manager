/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.patching

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.InstallerAbortDialog
import com.aliucord.manager.ui.screens.patching.components.*
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.util.paddings.*
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import com.aliucord.manager.ui.util.thenIf
import com.aliucord.manager.util.isIgnoringBatteryOptimizations
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.filter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

val VERTICAL_PADDING: Dp = 18.dp

@Parcelize
class PatchingScreen(private val data: PatchOptions) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Patching"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val model = getScreenModel<PatchingScreenModel> { parametersOf(data) }

        val state by model.state.collectAsState()
        val listState = rememberLazyListState()
        val showMinimizationWarning = rememberSaveable { !context.isIgnoringBatteryOptimizations() }

        // Exit warning dialog (dismiss itself if install process state changes, esp. for Success)
        var showAbortWarning by rememberSaveable(model.state.collectAsState().value) { mutableStateOf(false) }

        // The currently expanded step group on this screen
        var expandedGroup by rememberSaveable { mutableStateOf<StepGroup?>(StepGroup.Prepare) }

        // Only show exit warning if currently working
        val onTryExit: () -> Unit = remember {
            {
                // Show cancellation if currently running
                if (state == PatchingScreenState.Working && !model.devMode) {
                    showAbortWarning = true
                }
                // Go home directly if install was successful
                else if (state is PatchingScreenState.Success) {
                    navigator.popUntilRoot()
                }
                // Go back to the patch options screen
                else {
                    navigator.back(currentActivity = null)
                }
            }
        }

        // Prevent screen from turning off while working
        Wakelock(active = state is PatchingScreenState.Working)

        LaunchedEffect(state) {
            when (state) {
                // Go home directly if screen model mandates so (usually caused by cancelled PackageInstaller dialog)
                PatchingScreenState.CloseScreen -> navigator.popUntilRoot()

                // Close all groups when successfully finished everything
                PatchingScreenState.Success -> {
                    expandedGroup = null
                }

                else -> {}
            }

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

        Scaffold(
            topBar = { PatchingAppBar(onTryExit) },
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(VERTICAL_PADDING),
                modifier = Modifier
                    .padding(paddingValues.exclude(PaddingValuesSides.Bottom)),
            ) {
                if (state == PatchingScreenState.Working) {
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
                                text = stringResource(R.string.installer_banner_minimization),
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
                        BannerSection(visible = state is PatchingScreenState.Failed) {
                            val handler = LocalUriHandler.current

                            TextBanner(
                                text = stringResource(R.string.installer_banner_failure),
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

                    item(key = "INSTALLED_BANNER") {
                        BannerSection(visible = state is PatchingScreenState.Success) {
                            TextBanner(
                                text = stringResource(R.string.installer_banner_success),
                                icon = painterResource(R.drawable.ic_check_circle),
                                iconColor = Color(0xFF59B463),
                                outlineColor = MaterialTheme.colorScheme.surfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .padding(bottom = VERTICAL_PADDING)
                                    .fillMaxWidth(),
                            )
                        }
                    }

                    for ((group, steps) in model.steps?.entries ?: persistentListOf()) {
                        item(key = System.identityHashCode(group)) {
                            StepGroupCard(
                                name = stringResource(group.localizedName),
                                subSteps = steps,
                                isExpanded = expandedGroup == group,
                                onExpand = { expandedGroup = group },
                                modifier = Modifier
                                    .padding(bottom = VERTICAL_PADDING)
                                    .fillMaxWidth()
                                    .thenIf(state is PatchingScreenState.Success) { alpha(.5f) }
                            )
                        }
                    }

                    item(key = "BUTTONS") {
                        var cacheCleared by rememberSaveable { mutableStateOf(false) }
                        val filteredState by remember { model.state.filter { it.isProgressChange } }
                            .collectAsState(initial = PatchingScreenState.Working)

                        AnimatedVisibility(
                            visible = filteredState != PatchingScreenState.Working,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically { it * -2 },
                        ) {
                            Column {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .padding(bottom = VERTICAL_PADDING)
                                )

                                when (filteredState) {
                                    PatchingScreenState.Working -> {}
                                    PatchingScreenState.CloseScreen -> error("unreachable")

                                    PatchingScreenState.Success -> {
                                        MainActionButton(
                                            text = stringResource(R.string.action_launch_aliucord),
                                            icon = painterResource(R.drawable.ic_launch),
                                            onClick = model::launchApp,
                                        )
                                    }

                                    is PatchingScreenState.Failed -> {
                                        MainActionButton(
                                            text = stringResource(R.string.action_retry_install),
                                            icon = painterResource(R.drawable.ic_refresh),
                                            onClick = model::restart,
                                        )
                                    }
                                }

                                MainActionButton(
                                    text = stringResource(R.string.setting_clear_cache),
                                    icon = painterResource(R.drawable.ic_delete_forever),
                                    enabled = !cacheCleared,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ),
                                    onClick = {
                                        cacheCleared = true
                                        model.clearCache()
                                    },
                                    modifier = Modifier
                                        .padding(top = 14.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    item(key = "FUN_FACT") {
                        FunFact(
                            text = stringResource(model.funFact),
                            state = state,
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

// TODO: error log
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
