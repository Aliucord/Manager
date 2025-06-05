/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.*
import cafe.adriel.voyager.transitions.SlideTransition
import com.aliucord.manager.manager.OverlayManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.dialogs.StoragePermissionsDialog
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.widgets.updater.UpdaterDialog
import com.aliucord.manager.util.IS_CUSTOM_BUILD
import com.aliucord.manager.util.back
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesManager by inject()
    private val overlays: OverlayManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ManagerTheme(
                theme = preferences.theme,
                dynamicColor = preferences.dynamicColor,
            ) {
                StoragePermissionsDialog()

                if (!IS_CUSTOM_BUILD) {
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
}
