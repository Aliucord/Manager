package com.aliucord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.manager.InstallerSetting
import com.aliucord.manager.ui.screens.permissions.PermissionsScreenContent
import com.aliucord.manager.ui.theme.ManagerTheme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
fun PermissionsScreenPreview() {
    ManagerTheme {
        PermissionsScreenContent(
            installer = InstallerSetting.PackageInstaller,
            openInstallersDialog = {},
            storagePermsGranted = true,
            onGrantStoragePerms = {},
            unknownSourcesPermsGranted = true,
            onGrantUnknownSourcesPerms = {},
            notificationsPermsGranted = false,
            onGrantNotificationsPerms = {},
            batteryPermsGranted = false,
            onGrantBatteryPerms = {},
            canContinue = true,
            onContinue = {},
        )
    }
}
