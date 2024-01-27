package com.aliucord.manager.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.about.AboutScreen
import com.aliucord.manager.ui.screens.plugins.PluginsScreen
import com.aliucord.manager.ui.screens.settings.SettingsScreen

@Composable
fun HomeAppBar() {
    TopAppBar(
        title = {},
        actions = {
            val uriHandler = LocalUriHandler.current
            val navigator = LocalNavigator.currentOrThrow

            IconButton(
                onClick = {
                    uriHandler.openUri("https://discord.gg/${BuildConfig.SUPPORT_SERVER}")
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_discord),
                    contentDescription = stringResource(R.string.support_server)
                )
            }

            IconButton(onClick = { navigator.push(AboutScreen()) }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.navigation_about)
                )
            }

            IconButton(onClick = { navigator.push(PluginsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_extension),
                    contentDescription = stringResource(R.string.navigation_about)
                )
            }

            IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings)
                )
            }
        }
    )
}
