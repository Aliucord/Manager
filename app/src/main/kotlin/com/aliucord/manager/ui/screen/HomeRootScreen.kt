package com.aliucord.manager.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.navigation.AppDestination
import com.aliucord.manager.ui.navigation.HomeDestination
import com.xinto.taxi.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainRootScreen(navigator: BackstackNavigator<AppDestination>) {
    val mainRootNavigator = rememberNavigator(HomeDestination.HOME)
    val currentDestination = mainRootNavigator.currentDestination
    val orientation = LocalConfiguration.current.orientation
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(stringResource(mainRootNavigator.currentDestination.label))
                },
                actions = {
                    val localUriHandler = LocalUriHandler.current

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
                                    navigator.push(AppDestination.About)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "About"
                                    )
                                },
                                text = { Text(stringResource(R.string.about)) }
                            )

                            DropdownMenuItem(
                                onClick = {
                                    isMenuExpanded = false
                                    navigator.push(AppDestination.Settings)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                text = { Text(stringResource(R.string.settings)) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                NavigationBar {

                    HomeDestination.values().forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            icon = {
                                if (currentDestination == destination) Icon(
                                    destination.selectedIcon,
                                    stringResource(destination.label)
                                ) else Icon(destination.unselectedIcon, stringResource(destination.label))
                            },
                            label = { Text(stringResource(destination.label)) },
                            onClick = { mainRootNavigator.replace(destination) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                NavRail(
                    currentDestination = currentDestination,
                    onClickDestination = mainRootNavigator::replace
                )
            }

            Taxi(
                modifier = Modifier.weight(1f, true),
                navigator = mainRootNavigator,
                transitionSpec = { fadeIn() with fadeOut() }
            ) { destination ->
                when (destination) {
                    HomeDestination.HOME -> HomeScreen(navigator = navigator)
                    HomeDestination.PLUGINS -> PluginsScreen(navigator = navigator)
                }
            }
        }

    }
}

@Composable
fun NavRail(
    currentDestination: HomeDestination,
    onClickDestination: (HomeDestination) -> Unit
) = NavigationRail {
    HomeDestination.values().forEach { destination ->
        NavigationRailItem(
            selected = currentDestination == destination,
            icon = {
                if (currentDestination == destination) Icon(
                    destination.selectedIcon,
                    stringResource(destination.label)
                ) else Icon(destination.unselectedIcon, stringResource(destination.label))
            },
            label = { Text(stringResource(destination.label)) },
            onClick = { onClickDestination(destination) }
        )
    }
}
