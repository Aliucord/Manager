/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.content.Intent
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
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

        setContent {
            ManagerTheme(
                isDarkTheme = preferences.theme == Theme.DARK || preferences.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                isDynamicColor = preferences.dynamicColor
            ) {
                val navigator = rememberBackstackNavigator<AppDestination>(AppDestination.Home)
                var manageStorageGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }

                BackHandler {
                    if (!navigator.pop()) finish()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !manageStorageGranted) {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {
                            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                if (Environment.isExternalStorageManager())
                                    manageStorageGranted = true
                            }

                            Button(
                                onClick = {
                                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                        .setData("package:com.aliucord.manager".toUri())
                                        .let { launcher.launch(it) }
                                },
                            ) {
                                Text(stringResource(R.string.open_settings))
                            }
                        },
                        title = { Text(stringResource(R.string.permissions_grant)) },
                        text = { Text(stringResource(R.string.permissions_grant_body)) },
                        properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false
                        )
                    )
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
                            onClickBack = navigator::pop
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
