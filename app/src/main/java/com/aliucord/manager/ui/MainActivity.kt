/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.preferences.sharedPreferences
import com.aliucord.manager.ui.theme.AliucordManagerTheme
import com.aliucord.manager.ui.theme.primaryColor
import com.aliucord.manager.utils.Github
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE)
        Github.checkForUpdates()
        setContent {
            AliucordManagerTheme {
                MainActivityLayout()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainActivityLayout() {
    val systemUiController = rememberSystemUiController()
    val storagePermissionState = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val colors = MaterialTheme.colors
    SideEffect {
        systemUiController.setNavigationBarColor(colors.background)
        systemUiController.setStatusBarColor(colors.primary)
        if (!storagePermissionState.hasPermission) storagePermissionState.launchPermissionRequest()
    }

    val navController = rememberNavController()
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: Screen.Home.route
            TopAppBar(
                title = { Text(stringResource(Screen.SCREENS.find { it.route == route }!!.displayName)) },
                backgroundColor = primaryColor,
                contentColor = MaterialTheme.colors.onPrimary,
                navigationIcon = if (route != Screen.Home.route) {{
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                }} else null,
                actions = {
                    if (route != Screen.Home.route) return@TopAppBar
                    val context = LocalContext.current
                    IconButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/${BuildConfig.SUPPORT_SERVER}"))) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_discord),
                            contentDescription = stringResource(R.string.support_server),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Plugins.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        },
                        modifier = Modifier.padding(bottom = 2.dp, start = 8.dp) // Make up for icon being off center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plugin_24dp),
                            contentDescription = stringResource(id = R.string.launch_plugins),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.show_menu),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        DropdownMenuItem(onClick = {
                            isMenuExpanded = false
                            navController.navigate(Screen.About.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        }) {
                            Text(stringResource(Screen.About.displayName))
                        }
                        DropdownMenuItem(onClick = {
                            isMenuExpanded = false
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        }) {
                            Text(stringResource(Screen.Settings.displayName))
                        }
                    }
                }
            )
        }
    ) {
        PermissionRequired(
            permissionState = storagePermissionState,
            permissionNotGrantedContent = { GrantPermission(permissionState = storagePermissionState) },
            permissionNotAvailableContent = { GrantPermission(permissionState = storagePermissionState) }
        ) {
            NavHost(
                navController,
                startDestination = Screen.Home.route
            ) {
                for (screen in Screen.SCREENS) {
                    composable(screen.route) { screen.content() }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GrantPermission(permissionState: PermissionState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.permission_required), style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Row {
            val context = LocalContext.current

            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.permission_grant))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    ))
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray, contentColor = Color.Black)
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}
