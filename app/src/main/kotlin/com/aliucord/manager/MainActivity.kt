/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.dialog.StoragePermissionsDialog
import com.aliucord.manager.ui.navigation.AppDestination
import com.aliucord.manager.ui.navigation.HomeDestination
import com.aliucord.manager.ui.screen.*
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import com.xinto.taxi.*
import org.koin.android.ext.android.inject

val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesManager by inject()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ManagerTheme(
                isDarkTheme = preferences.theme == Theme.DARK || preferences.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                isDynamicColor = preferences.dynamicColor
            ) {
                val navigator = rememberBackstackNavigator<AppDestination>(AppDestination.Home)
                val homeRootNavigator = rememberNavigator(HomeDestination.HOME)

                BackHandler {
                    if (!navigator.pop()) {
                        if (homeRootNavigator.currentDestination == HomeDestination.HOME)
                            finish()
                        else
                            homeRootNavigator.replace(HomeDestination.HOME)
                    }
                }

                StoragePermissionsDialog()

                Taxi(
                    modifier = Modifier.fillMaxSize(),
                    navigator = navigator,
                    transitionSpec = { fadeIn() with fadeOut() }
                ) { destination ->
                    when (destination) {
                        is AppDestination.Home -> MainRootScreen(
                            mainRootNavigator = homeRootNavigator,
                            onInstallClick = { navigator.push(AppDestination.Install(it)) },
                            onAboutClick = { navigator.push(AppDestination.About) }
                        )

                        is AppDestination.Install -> InstallerScreen(
                            installData = destination.installData,
                            onBackClick = navigator::pop
                        )

                        is AppDestination.About -> AboutScreen(
                            onBackClick = navigator::pop
                        )

                        else -> {}
                    }
                }
            }
        }
    }
}
