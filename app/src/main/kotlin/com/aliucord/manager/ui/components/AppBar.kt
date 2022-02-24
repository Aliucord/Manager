package com.aliucord.manager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.Screen

@ExperimentalFoundationApi
@Composable
fun AppBar(navController: NavController) {
    val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: Screen.Home.route
    var isMenuExpanded by remember { mutableStateOf(false) }

    MediumTopAppBar(
        navigationIcon = {
            if (route != Screen.Home.route) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        title = {
            Text(stringResource(Screen.screens.first { screen ->
                screen.route == route
            }.displayName))
        },
        actions = {
            val localUriHandler = LocalUriHandler.current

            IconButton(
                onClick = {
                    navController.navigate(Screen.Store.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
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
                    modifier = Modifier.wrapContentWidth(),
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            navController.navigate(Screen.About.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        },
                        text = { Text(stringResource(Screen.About.displayName)) }
                    )

                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        },
                        text = { Text(stringResource(Screen.Settings.displayName)) }
                    )
                }
            }
        }
    )
}