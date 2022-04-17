/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.destinations.*
import com.aliucord.manager.ui.screens.navDestination
import com.ramcosta.composedestinations.navigation.navigateTo
import com.ramcosta.composedestinations.navigation.popUpTo

@get:StringRes
private val Destination.title
    get() = when (this) {
        HomeScreenDestination -> R.string.home
        InstallerScreenDestination -> R.string.installer
        CommitsScreenDestination -> R.string.commits
        StoreScreenDestination -> R.string.store
        SettingsScreenDestination -> R.string.settings
        AboutScreenDestination -> R.string.about
    }

@Composable
fun AppBar(navController: NavController) {
    val destination = navController.currentBackStackEntryAsState().value?.navDestination ?: HomeScreenDestination
    var isMenuExpanded by remember { mutableStateOf(false) }

    SmallTopAppBar(
        navigationIcon = {
            if (destination != HomeScreenDestination) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        title = {
            Text(stringResource(destination.title))
        },
        actions = {
            val localUriHandler = LocalUriHandler.current

            IconButton(
                onClick = {
                    navController.navigateTo(StoreScreenDestination) {
                        popUpTo(HomeScreenDestination) { saveState = true }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plugin_24dp),
                    contentDescription = stringResource(R.string.plugins),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    localUriHandler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}")
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_discord),
                    contentDescription = stringResource(R.string.support_server),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { isMenuExpanded = !isMenuExpanded }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.show_menu),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    offset = DpOffset(x = 12.dp, y = 0.dp),
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            navController.navigateTo(AboutScreenDestination)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About"
                            )
                        },
                        text = { Text(stringResource(AboutScreenDestination.title)) }
                    )

                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            navController.navigateTo(SettingsScreenDestination)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        text = { Text(stringResource(SettingsScreenDestination.title)) }
                    )
                }
            }
        }
    )
}
