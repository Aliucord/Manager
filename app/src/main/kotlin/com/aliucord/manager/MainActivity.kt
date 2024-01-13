/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.dialog.StoragePermissionsDialog
import com.aliucord.manager.ui.dialog.UpdaterDialog
import com.aliucord.manager.ui.navigation.*
import com.aliucord.manager.ui.screen.*
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import dev.olshevski.navigation.reimagined.*
import kotlinx.collections.immutable.persistentListOf
import org.koin.android.ext.android.inject

val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ManagerTheme(
                isDarkTheme = preferences.theme == Theme.DARK || preferences.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                isDynamicColor = preferences.dynamicColor
            ) {
                val navController = rememberNavController<AppDestination>(AppDestination.Home)

                BackHandler {
                    navController.back()
                }

                StoragePermissionsDialog()

                @Suppress("KotlinConstantConditions")
                if (
                    BuildConfig.GIT_BRANCH == "release" &&
                    !BuildConfig.GIT_LOCAL_CHANGES &&
                    !BuildConfig.GIT_LOCAL_COMMITS
                ) {
                    UpdaterDialog()
                }

                NavHost(
                    controller = navController,
                ) {
                    when (val dest = this.currentHostEntry.destination) {
                        is BaseScreenDestination -> BaseScreen(
                            currentNavItem = dest,
                            bottomNavItems = persistentListOf(AppDestination.Home, AppDestination.Plugins, AppDestination.Settings),
                            onNavChanged = { navController.replaceLast(it) }
                        ) {
                            when (dest) {
                                is AppDestination.Home -> HomeScreen(
                                    onClickInstall = { data ->
                                        navController.navigate(AppDestination.Install(data))
                                    }
                                )
                                is AppDestination.Plugins -> PluginsScreen()
                                is AppDestination.Settings -> SettingsScreen()
                            }
                        }
                        is AppDestination.Install -> InstallerScreen(
                            installData = dest.installData,
                            onBackClick = { navController.back() }
                        )

                        is AppDestination.About -> AboutScreen(
                            onBackClick = { navController.back() }
                        )
                    }
                }
            }
        }
    }
}
