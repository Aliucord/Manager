/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.StoragePermissionsDialog
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.screens.patching.PatchingScreen
import com.aliucord.manager.ui.screens.patchopts.PatchOptionsScreen
import com.aliucord.manager.ui.widgets.updater.UpdaterDialog
import com.aliucord.manager.util.IS_CUSTOM_BUILD
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ManagerTheme(
                isDarkTheme = preferences.theme == Theme.DARK || preferences.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                isDynamicColor = preferences.dynamicColor
            ) {
                StoragePermissionsDialog()

                if (!IS_CUSTOM_BUILD) {
                    UpdaterDialog()
                }

                Navigator(
                    screen = HomeScreen(),
                    onBackPressed = null,
                ) { navigator ->
                    BackHandler {
                        navigator.back(this@MainActivity)
                    }

                    ScreenTransition(
                        navigator = navigator,
                        transition = block@{
                            val fadeIn = fadeIn(tween(durationMillis = 300))
                            val fadeOut = fadeOut(tween(durationMillis = 750, delayMillis = 200))

                            when {
                                // Going from Home -> InstallOptions
                                initialState is HomeScreen && targetState is PatchOptionsScreen ->
                                    slideInVertically { (it * 1.5).toInt() } + fadeIn togetherWith fadeOut
                                // Going from InstallOptions -> Home
                                initialState is PatchOptionsScreen && targetState is HomeScreen ->
                                    fadeIn togetherWith slideOutVertically { it * 2 } + fadeOut
                                // Going from InstallOptions -> Install
                                initialState is PatchOptionsScreen && targetState is PatchingScreen ->
                                    slideInHorizontally { it * 2 } + fadeIn togetherWith fadeOut
                                // Going from Install -> InstallOptions
                                initialState is PatchingScreen && targetState is PatchOptionsScreen ->
                                    fadeIn togetherWith slideOutHorizontally { it * 2 } + fadeOut

                                else -> fadeIn togetherWith fadeOut
                            }
                        }
                    )
                }
            }
        }
    }
}
