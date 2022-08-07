/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager

import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.preferences.sharedPreferences
import com.aliucord.manager.ui.navigation.AppDestination
import com.aliucord.manager.ui.screen.*
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import com.xinto.taxi.Taxi
import com.xinto.taxi.rememberBackstackNavigator

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData("package:com.aliucord.manager".toUri())

            startActivity(intent)
        }

        setContent {
            ManagerTheme(
                isBlack = Prefs.useBlack.get(),
                isDarkTheme = run {
                    val theme = Theme.from(Prefs.theme.get())

                    theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK
                }
            ) {
                val navigator = rememberBackstackNavigator<AppDestination>(AppDestination.Home)

                BackHandler {
                    navigator.pop()
                }

                Taxi(
                    modifier = Modifier.fillMaxSize(),
                    navigator = navigator,
                    transitionSpec = { fadeIn() with fadeOut() }
                ) { destination ->
                    when (destination) {
                        is AppDestination.Home -> MainRootScreen(
                            navigator = navigator
                        )
                        is AppDestination.Plugins -> PluginsScreen(
                            navigator = navigator
                        )
                        is AppDestination.Install -> InstallerScreen(
                            onClickBack = navigator::pop,
                        )
                        is AppDestination.Settings -> SettingsScreen(
                            onClickBack = navigator::pop
                        )
                        is AppDestination.About -> AboutScreen(
                            onClickBack = navigator::pop
                        )
                        is AppDestination.Store -> StoreScreen(
                            onClickBack = navigator::pop
                        )
                    }
                }
            }
        }
    }
}
