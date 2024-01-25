/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.StoragePermissionsDialog
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.widgets.updater.UpdaterDialog
import org.koin.android.ext.android.inject

// TODO: move to a path provider in DI
val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")

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

                @Suppress("KotlinConstantConditions")
                if (
                    BuildConfig.GIT_BRANCH == "release" &&
                    !BuildConfig.GIT_LOCAL_CHANGES &&
                    !BuildConfig.GIT_LOCAL_COMMITS
                ) {
                    UpdaterDialog()
                }

                Navigator(
                    screen = HomeScreen(),
                    onBackPressed = null,
                ) { navigator ->
                    BackHandler {
                        navigator.back(this@MainActivity)
                    }

                    FadeTransition(navigator)
                }
            }
        }
    }
}
