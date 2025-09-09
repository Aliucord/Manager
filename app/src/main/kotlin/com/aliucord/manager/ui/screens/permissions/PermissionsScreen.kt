package com.aliucord.manager.ui.screens.permissions

import android.os.Build
import android.os.Parcelable
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.screens.permissions.components.PermissionButton
import com.aliucord.manager.ui.screens.permissions.components.PermissionsAppBar
import com.aliucord.manager.ui.util.paddings.*
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinActivityViewModel

@Parcelize
@OptIn(ExperimentalVoyagerApi::class)
class PermissionsScreen : Screen, ScreenTransition, Parcelable {
    @IgnoredOnParcel
    override val key = "Permissions"

    override fun enter(lastEvent: StackEvent): EnterTransition? = EnterTransition.None

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinActivityViewModel<PermissionsModel>()

        // Go back (ex: HomeScreen) when all permissions have been granted
        LaunchedEffect(model.allPermsGranted) {
            if (model.allPermsGranted)
                navigator.pop()
        }

        PermissionsScreenContent(
            storagePermsGranted = model.storagePermsGranted,
            onGrantStoragePerms = if (Build.VERSION.SDK_INT >= 30) {
                model::requestManageStoragePerms
            } else {
                model::requestStoragePerms
            },
            installPermsGranted = model.installPermsGranted,
            onGrantInstallPerms = model::requestInstallPerms,
            notificationsPermsGranted = model.notificationsPermsGranted,
            onGrantNotificationsPerms = model::requestNotificationsPerms,
            batteryPermsGranted = model.batteryPermsGranted,
            onGrantBatteryPerms = model::grantBatteryPerms,
            canContinue = model.requiredPermsGranted,
            onContinue = { navigator.replace(HomeScreen()) },
        )
    }
}

@Composable
fun PermissionsScreenContent(
    storagePermsGranted: Boolean,
    onGrantStoragePerms: () -> Unit,
    installPermsGranted: Boolean,
    onGrantInstallPerms: () -> Unit,
    notificationsPermsGranted: Boolean,
    onGrantNotificationsPerms: () -> Unit,
    batteryPermsGranted: Boolean,
    onGrantBatteryPerms: () -> Unit,
    canContinue: Boolean,
    onContinue: () -> Unit,
) {
    Scaffold(
        topBar = { PermissionsAppBar() },
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedByLastAtBottom(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = padding
                .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top)
                .add(PaddingValues(vertical = 12.dp)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(padding.exclude(PaddingValuesSides.Bottom))
        ) {
            item(key = "PERMS_INSTALL", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = stringResource(R.string.permissions_install_title),
                    description = stringResource(R.string.permissions_install_desc),
                    granted = installPermsGranted,
                    required = true,
                    icon = painterResource(R.drawable.ic_apk_install),
                    onClick = onGrantInstallPerms,
                )
            }

            item(key = "PERMS_STORAGE", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = stringResource(R.string.permissions_storage_title),
                    description = stringResource(R.string.permissions_storage_desc),
                    granted = storagePermsGranted,
                    required = true,
                    icon = painterResource(R.drawable.ic_save),
                    onClick = onGrantStoragePerms,
                )
            }

            item(key = "PERMS_NOTIFICATIONS", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = stringResource(R.string.permissions_notifs_title),
                    description = stringResource(R.string.permissions_notifs_desc),
                    granted = notificationsPermsGranted,
                    required = false,
                    icon = painterResource(R.drawable.ic_bell),
                    onClick = onGrantNotificationsPerms,
                )
            }

            item(key = "PERMS_BATTERY", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = stringResource(R.string.permissions_battery_title),
                    description = stringResource(R.string.permissions_battery_desc),
                    granted = batteryPermsGranted,
                    required = false,
                    icon = painterResource(R.drawable.ic_battery_settings),
                    onClick = onGrantBatteryPerms,
                )
            }

            item(key = "LEGEND") {
                Text(
                    text = stringResource(R.string.permissions_legend, "＊"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                )
            }

            item(key = "CONTINUE") {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    FilledTonalButton(
                        onClick = onContinue,
                        enabled = canContinue,
                    ) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            }
        }
    }
}
