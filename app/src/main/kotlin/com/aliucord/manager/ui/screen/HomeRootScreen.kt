package com.aliucord.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.navigation.HomeDestination
import com.xinto.taxi.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainRootScreen(
    mainRootNavigator: RegularNavigator<HomeDestination>,
    onClickInstall: (InstallData) -> Unit,
    onClickAbout: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(mainRootNavigator.currentDestination.label),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                },
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

                    IconButton(onClick = onClickAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.about)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val currentDestination = mainRootNavigator.currentDestination

            NavigationBar {
                HomeDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        icon = {
                            if (currentDestination == destination) {
                                Icon(
                                    imageVector = destination.selectedIcon,
                                    contentDescription = stringResource(destination.label)
                                )
                            } else {
                                Icon(
                                    imageVector = destination.unselectedIcon,
                                    contentDescription = stringResource(destination.label)
                                )
                            }
                        },
                        label = { Text(stringResource(destination.label)) },
                        onClick = { mainRootNavigator.replace(destination) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Taxi(
            modifier = Modifier.padding(paddingValues),
            navigator = mainRootNavigator,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { destination ->
            when (destination) {
                HomeDestination.HOME -> HomeScreen(
                    onClickInstall = onClickInstall
                )

                HomeDestination.PLUGINS -> PluginsScreen()
                HomeDestination.SETTINGS -> SettingsScreen()
            }
        }
    }
}
