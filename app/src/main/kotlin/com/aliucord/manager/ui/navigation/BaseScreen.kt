@file:OptIn(ExperimentalMaterial3Api::class)

package com.aliucord.manager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.screen.*

@Composable
fun BaseScreen(
    currentNavItem: BaseScreenDestination,
    bottomNavItems: List<BaseScreenDestination>,
    onNavChanged: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentNavItem.label)) },
                actions = {
                    val localUriHandler = LocalUriHandler.current

                    IconButton(
                        onClick = {
                            localUriHandler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_discord),
                            contentDescription = stringResource(R.string.support_server)
                        )
                    }

                    IconButton(onClick = { onNavChanged(AppDestination.About) }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.navigation_about)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                for (navItem in bottomNavItems) {
                    NavigationBarItem(
                        selected = currentNavItem == navItem,
                        icon = {
                            Icon(
                                imageVector = if (currentNavItem == navItem) {
                                    navItem.selectedIcon
                                } else {
                                    navItem.unselectedIcon
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(navItem.label)) },
                        onClick = { onNavChanged(navItem) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            content()
        }
    }
}
