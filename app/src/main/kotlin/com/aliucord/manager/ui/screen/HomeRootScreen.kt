package com.aliucord.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.ui.navigation.HomeDestination
import com.xinto.taxi.RegularNavigator
import com.xinto.taxi.Taxi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainRootScreen(
    mainRootNavigator: RegularNavigator<HomeDestination>,
    onInstallClick: (InstallData) -> Unit,
    onAboutClick: () -> Unit
) {

    Scaffold(
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
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            navigator = mainRootNavigator,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { destination ->
            when (destination) {
                HomeDestination.HOME -> HomeScreen(
                    onClickInstall = onInstallClick
                )

                HomeDestination.PLUGINS -> PluginsScreen()
                HomeDestination.SETTINGS -> SettingsScreen(
                    onAboutClick = onAboutClick
                )
            }
        }
    }
}
