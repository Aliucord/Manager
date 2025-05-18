package com.aliucord.manager.ui.screens.home.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.about.AboutScreen
import com.aliucord.manager.ui.screens.logs.LogsListScreen
import com.aliucord.manager.ui.screens.settings.SettingsScreen

@Composable
fun HomeAppBar() {
    TopAppBar(
        title = {},
        actions = {
            val navigator = LocalNavigator.current

            IconButton(onClick = { navigator?.push(AboutScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.navigation_about),
                )
            }

            IconButton(onClick = { navigator?.push(LogsListScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_receipt),
                    contentDescription = stringResource(R.string.navigation_about),
                )
            }

            IconButton(onClick = { navigator?.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings),
                )
            }
        }
    )
}
