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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.*
import cafe.adriel.voyager.transitions.ScreenTransition
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.StoragePermissionsDialog
import com.aliucord.manager.ui.screens.home.HomeScreen
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

                        ScreenTransition(
                            navigator = navigator,
                            transition = { unifiedScreenTransition(navigator.lastEvent) }
                        )
                    }
                }
            }
        }
    }

    private fun unifiedScreenTransition(lastEvent: StackEvent): ContentTransform = when (lastEvent) {
        StackEvent.Push, StackEvent.Replace -> ContentTransform(
            targetContentEnter = slideInHorizontally { it * 2 } + fadeIn(tween(durationMillis = 400)),
            initialContentExit = fadeOut(tween(durationMillis = 400, delayMillis = 300)),
        )

        StackEvent.Pop -> ContentTransform(
            targetContentEnter = EnterTransition.None,
            targetContentZIndex = -1f,
            initialContentExit = slideOutHorizontally { it * 2 } + fadeOut(tween(durationMillis = 400, delayMillis = 300)),
        )

        StackEvent.Idle -> fadeIn() togetherWith fadeOut()
    }
}
