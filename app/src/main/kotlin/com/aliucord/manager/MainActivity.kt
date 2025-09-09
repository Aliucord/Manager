/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.*
import cafe.adriel.voyager.transitions.SlideTransition
import com.aliucord.manager.MainActivity.Companion.EXTRA_PACKAGE_NAME
import com.aliucord.manager.manager.OverlayManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.patcher.InstallMetadata
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.screens.patching.PatchingScreen
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.screens.permissions.PermissionsModel
import com.aliucord.manager.ui.screens.permissions.PermissionsScreen
import com.aliucord.manager.ui.screens.plugins.PluginsScreen
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.widgets.updater.UpdaterDialog
import com.aliucord.manager.util.*
import com.github.diamondminer88.zip.ZipReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val permissions: PermissionsModel by viewModel()
    private val preferences: PreferencesManager by inject()
    private val overlays: OverlayManager by inject()
    private val json: Json by inject()
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            // Refresh permissions when the activity resumes
            LifecycleResumeEffect(Unit) {
                permissions.refresh()

                onPauseOrDispose {}
            }

            ManagerTheme(
                theme = preferences.theme,
                dynamicColor = preferences.dynamicColor,
            ) {
                if (BuildConfig.RELEASE) {
                    UpdaterDialog()
                }

                @OptIn(ExperimentalVoyagerApi::class)
                CompositionLocalProvider(
                    LocalNavigatorSaver provides parcelableNavigatorSaver(),
                ) {
                    Navigator(
                        screen = HomeScreen(),
                        onBackPressed = null,
                    ) { navigator ->
                        // Open the permissions screen whenever permissions are insufficient
                        LaunchedEffect(permissions.requiredPermsGranted) {
                            if (!permissions.requiredPermsGranted)
                                navigator.pushOnce(PermissionsScreen())
                        }

                        DisposableEffect(Unit) {
                            this@MainActivity.intent?.let { handleNewIntent(it, navigator) }

                            fun handle(intent: Intent) = handleNewIntent(intent, navigator)
                            addOnNewIntentListener(::handle)
                            onDispose { removeOnNewIntentListener(::handle) }
                        }

                        BackHandler {
                            navigator.back(this@MainActivity)
                        }

                        SlideTransition(
                            navigator = navigator,
                            disposeScreenAfterTransitionEnd = true,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMedium,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            )
                        )
                    }

                    overlays.Overlays()
                }
            }
        }
    }

    private fun handleNewIntent(intent: Intent, navigator: Navigator) = scope.launchBlock {
        when (intent.action) {
            INTENT_REINSTALL -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
                    Log.w(BuildConfig.TAG, "Missing $EXTRA_PACKAGE_NAME extra for intent $INTENT_REINSTALL")
                    return@launchBlock
                }

                navigator.push(handleReinstall(packageName))
            }

            INTENT_OPEN_PLUGINS -> {
                // TODO: per-install plugins screen
                // val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
                //     Log.w(BuildConfig.TAG, "Missing $EXTRA_PACKAGE_NAME extra for intent $INTENT_REINSTALL")
                //     return@launchBlock
                // }

                navigator.push(PluginsScreen())
            }

            else -> {
                Log.w(BuildConfig.TAG, "Unhandled intent ${intent.action}")
            }
        }
    }

    private suspend fun handleReinstall(packageName: String): Screen {
        val metadata = try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val metadataFile = ZipReader(applicationInfo.publicSourceDir)
                .use { it.openEntry("aliucord.json")?.read() }

            @OptIn(ExperimentalSerializationApi::class)
            metadataFile?.let { json.decodeFromStream<InstallMetadata>(it.inputStream()) }
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to parse Aliucord install metadata from package $packageName", t)
            mainThread { showToast(R.string.intent_reinstall_fail) }
            null
        }

        val patchOptions = metadata?.options
            ?: PatchOptions.Default.copy(packageName = packageName)

        return PatchingScreen(patchOptions)
    }

    companion object {
        /**
         * Intent action to trigger an immediate reinstallation of a specific Aliucord installation.
         * Required extra data:
         * - [EXTRA_PACKAGE_NAME] The target installation package name.
         */
        const val INTENT_REINSTALL = "com.aliucord.manager.REINSTALL"

        /**
         * Opens the plugins page for a specific Aliucord installation.
         * Required extra data:
         * - [EXTRA_PACKAGE_NAME] The target installation package name.
         */
        const val INTENT_OPEN_PLUGINS = "com.aliucord.manager.OPEN_PLUGINS"

        /**
         * Specifies the target package name for an action.
         */
        const val EXTRA_PACKAGE_NAME = "aliucord.packageName"
    }
}
