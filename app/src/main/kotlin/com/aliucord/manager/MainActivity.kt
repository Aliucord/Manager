/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

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
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.navigation.AppDestination
import com.aliucord.manager.ui.screen.*
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import com.xinto.taxi.Taxi
import com.xinto.taxi.rememberBackstackNavigator
import org.koin.android.ext.android.inject

val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesManager by inject()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData("package:com.aliucord.manager".toUri())

            startActivity(intent)
        }

        setContent {
            ManagerTheme(
                isDarkTheme = preferences.theme == Theme.DARK || preferences.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                isDynamicColor = preferences.dynamicColor
            ) {
                val navigator = rememberBackstackNavigator<AppDestination>(AppDestination.Home)

                BackHandler {
                    if (!navigator.pop()) finish()
                }

                Taxi(
                    modifier = Modifier.fillMaxSize(),
                    navigator = navigator,
                    transitionSpec = { fadeIn() with fadeOut() }
                ) { destination ->
                    when (destination) {
                        is AppDestination.Home -> MainRootScreen(
                            onClickInstall = { navigator.push(AppDestination.Install(it)) },
                            onClickAbout = { navigator.push(AppDestination.About) },
                            onClickSettings = { navigator.push(AppDestination.Settings) }
                        )
                        is AppDestination.Install -> InstallerScreen(
                            installData = destination.installData,
                            onClickBack = navigator::pop,
                        )
                        is AppDestination.Settings -> SettingsScreen(
                            onClickBack = navigator::pop
                        )
                        is AppDestination.About -> AboutScreen(
                            onClickBack = navigator::pop
                        )
                    }
                }
            }
        }
    }
}
