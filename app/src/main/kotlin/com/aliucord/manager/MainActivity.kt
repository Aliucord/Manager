/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager

import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.preferences.sharedPreferences
import com.aliucord.manager.ui.components.ManagerScaffold
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setOnExitAnimationListener { provider ->
            provider.view.animate().apply {
                interpolator = AccelerateInterpolator()
                duration = 200L

                alpha(0f)
                withEndAction(provider::remove)
                start()
            }
        }

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
                ManagerScaffold()
            }
        }
    }
}
