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
import com.aliucord.manager.MainActivity.Companion.EXTRA_COMPONENT_TYPE
import com.aliucord.manager.MainActivity.Companion.EXTRA_FILE_PATH
import com.aliucord.manager.MainActivity.Companion.EXTRA_PACKAGE_NAME
import com.aliucord.manager.manager.*
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
import java.io.File

class MainActivity : ComponentActivity() {
    private val permissions: PermissionsModel by viewModel()
    private val preferences: PreferencesManager by inject()
    private val overlays: OverlayManager by inject()
    private val paths: PathManager by inject()
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

            INTENT_IMPORT_COMPONENT -> {
                val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
                    Log.w(BuildConfig.TAG, "Missing $EXTRA_FILE_PATH extra for intent $INTENT_IMPORT_COMPONENT")
                    mainThread { showToast(R.string.intent_import_component_failure) }
                    return@launchBlock
                }
                val componentType = intent.getStringExtra(EXTRA_COMPONENT_TYPE) ?: run {
                    Log.w(BuildConfig.TAG, "Missing $EXTRA_COMPONENT_TYPE extra for intent $INTENT_IMPORT_COMPONENT")
                    mainThread { showToast(R.string.intent_import_component_failure) }
                    return@launchBlock
                }

                val file = File("/data/local/tmp", path)
                if (!file.exists()) {
                    Log.w(BuildConfig.TAG, "Intent $INTENT_IMPORT_COMPONENT specified an invalid file!")
                    mainThread { showToast(R.string.intent_import_component_failure) }
                    return@launchBlock
                }

                val targetDir = when (componentType) {
                    "injector" -> paths.customInjectorsDir
                    "patches" -> paths.customPatchesDir
                    else -> {
                        Log.w(BuildConfig.TAG, "Extra $EXTRA_COMPONENT_TYPE is not a valid value!")
                        mainThread { showToast(R.string.intent_import_component_failure) }
                        return@launchBlock
                    }
                }

                try {
                    file.copyTo(targetDir.resolve(file.name), overwrite = true)
                    file.delete() // This most likely silently fails
                } catch (e: Exception) {
                    Log.e(BuildConfig.TAG, "Failed to import custom component", e)
                    mainThread { showToast(R.string.intent_import_component_failure) }
                }

                mainThread { showToast(R.string.intent_import_component_success, file.name) }
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
         * Imports a custom component that was pushed to the device.
         * Required extra data:
         * - [EXTRA_COMPONENT_TYPE]: The type of the new component.
         * - [EXTRA_FILE_PATH]: The pushed component's file path under `/data/local/tmp`.
         */
        const val INTENT_IMPORT_COMPONENT = "com.aliucord.manager.IMPORT_COMPONENT"

        /**
         * Specifies the target package name for an action.
         */
        const val EXTRA_PACKAGE_NAME = "aliucord.packageName"

        /**
         * Specifies the target file path for an action.
         */
        const val EXTRA_FILE_PATH = "aliucord.file"

        /**
         * Specifies the custom component type for an action.
         * Can be one of the following values:
         * - `injector`
         * - `patches`
         */
        const val EXTRA_COMPONENT_TYPE = "aliucord.customComponentType"
    }
}
