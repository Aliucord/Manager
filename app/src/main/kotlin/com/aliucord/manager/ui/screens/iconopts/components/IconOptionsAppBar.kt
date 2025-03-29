package com.aliucord.manager.ui.screens.iconopts.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.screens.settings.SettingsScreen

@Composable
fun IconOptionsAppBar() {
    TopAppBar(
        navigationIcon = { BackButton() },
        title = { Text(stringResource(R.string.iconopts_screen_title)) },
        actions = {
            val navigator = LocalNavigator.currentOrThrow

            IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings)
                )
            }
        }
    )
}
