/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aliucord.manager.preferences.Preferences
import com.aliucord.manager.preferences.sharedPreferences
import com.aliucord.manager.preferences.themePref
import com.aliucord.manager.ui.Screen
import com.aliucord.manager.ui.components.AppBar
import com.aliucord.manager.ui.components.GrantPermission
import com.aliucord.manager.ui.screens.*
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.utils.Github
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState

@ExperimentalMaterialApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        Github.checkForUpdates()

        setContent {
            ManagerTheme(
                isBlack = Preferences.useBlack.get(),
                isDarkTheme = run {
                    val theme = Theme.from(themePref.get())

                    theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK
                }
            ) {
                MainActivityLayout()
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalPermissionsApi
@Composable
private fun MainActivityLayout() {
    val storagePermissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    val navController = rememberNavController()
    val requestAllFilesAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val context = LocalContext.current

    SideEffect {
        @SuppressLint("NewApi")
        if (requestAllFilesAccess && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData("package:com.aliucord.manager".toUri())

            context.startActivity(intent)
        } else if (storagePermissionState.shouldShowRationale) storagePermissionState.launchPermissionRequest()
    }

    Scaffold(
        topBar = { AppBar(navController) }
    ) { paddingValues ->
        PermissionRequired(
            permissionState = storagePermissionState,
            permissionNotAvailableContent = { GrantPermission(storagePermissionState) },
            permissionNotGrantedContent = { GrantPermission(storagePermissionState) }
        ) {
            NavHost(
                navController,
                modifier = Modifier.padding(paddingValues).padding(12.dp),
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) { HomeScreen(navController) }
                composable(Screen.Commits.route) { CommitsScreen() }
                composable(Screen.About.route) { AboutScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.Store.route) { StoreScreen() }
                composable(Screen.Installer.route) { InstallerScreen(navController) }
            }
        }
    }
}