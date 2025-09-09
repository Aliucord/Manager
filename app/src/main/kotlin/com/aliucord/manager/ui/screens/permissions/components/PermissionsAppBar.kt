package com.aliucord.manager.ui.screens.permissions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.settings.SettingsScreen

@Composable
fun PermissionsAppBar() {
    LargeTopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    text = "App Permissions",
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "Aliucord Manager requires permissions:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(.6f),
                    ),
                )
            }
        },
        actions = {
            val navigator = LocalNavigator.current

            IconButton(onClick = { navigator?.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings)
                )
            }
        }
    )
}
