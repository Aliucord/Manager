package com.aliucord.manager.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.about.AboutScreen
import com.aliucord.manager.ui.screens.settings.SettingsScreen

@Composable
fun HomeAppBar() {
    TopAppBar(
        title = {},
        actions = {
            val navigator = LocalNavigator.currentOrThrow

            IconButton(onClick = { navigator.push(AboutScreen()) }) {
                Icon(
                    imageVector = Icons.Default.Info,
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
