package com.aliucord.manager.ui.screens.permissions

import android.os.Build
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ProjectHeader
import com.aliucord.manager.ui.screens.home.HomeScreen
import com.aliucord.manager.ui.screens.permissions.components.PermissionButton
import com.aliucord.manager.ui.util.paddings.*
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinViewModel

@Parcelize
class PermissionsScreen : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Permissions"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Can't use koinActivityViewModel due to https://github.com/InsertKoinIO/koin/pull/2229
        val model = koinViewModel<PermissionsModel>(
            viewModelStoreOwner = LocalActivity.current as ComponentActivity,
        )

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
            onGrantNotificationsPerms = if (Build.VERSION.SDK_INT > 33) {
                model::requestNotificationsPerms
            } else {
                {}
            },
            batteryPermsGranted = model.batteryPermsGranted,
            onGrantBatteryPerms = if (Build.VERSION.SDK_INT > 23) {
                model::grantBatteryPerms
            } else {
                {}
            },
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
    Scaffold { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedByLastAtBottom(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = padding
                .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top)
                .add(PaddingValues(bottom = 12.dp)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(padding.exclude(PaddingValuesSides.Bottom))
        ) {
            item(key = "HEADER") {
                ProjectHeader(
                    modifier = Modifier.padding(top = 56.dp, bottom = 16.dp),
                )
            }

            item(key = "HEADER_2") {
                Text(
                    text = "Aliucord Manager requires the following permissions:",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            item(key = "PERMS_INSTALL", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = "Install Permissions",
                    description = "Manager requires install permissions to install Aliucord.",
                    granted = installPermsGranted,
                    icon = painterResource(R.drawable.ic_apk_install),
                    onClick = onGrantInstallPerms,
                )
            }

            item(key = "PERMS_STORAGE", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = "External Storage Permissions",
                    description = "Aliucord stores shared data in ~/Aliucord, which requires full storage permissions. Scoped storage is not currently implemented.",
                    granted = storagePermsGranted,
                    icon = painterResource(R.drawable.ic_save),
                    onClick = onGrantStoragePerms,
                )
            }

            item(key = "PERMS_NOTIFICATIONS", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = "Notifications Permissions",
                    description = "Used to show download progress if the app is minimized during installation.",
                    granted = notificationsPermsGranted,
                    icon = painterResource(R.drawable.ic_bell),
                    onClick = onGrantNotificationsPerms,
                )
            }

            item(key = "PERMS_BATTERY", contentType = "PERMISSION_BUTTON") {
                PermissionButton(
                    name = "Background Battery Permissions",
                    description = "Used to ensure the patching process does not get cancelled if the app is minimized.",
                    granted = batteryPermsGranted,
                    icon = painterResource(R.drawable.ic_battery_settings),
                    onClick = onGrantBatteryPerms,
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
