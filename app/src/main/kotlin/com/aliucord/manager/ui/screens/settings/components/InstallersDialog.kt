package com.aliucord.manager.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.manager.*
import com.aliucord.manager.util.showToast
import com.topjohnwu.superuser.Shell
import org.koin.compose.koinInject

@Composable
fun InstallersDialog(
    currentInstaller: InstallerSetting,
    onDismiss: () -> Unit,
    onConfirm: (InstallerSetting) -> Unit,
) {
    val context = LocalContext.current
    val shizuku = koinInject<ShizukuManager>()
    val dhizuku = koinInject<DhizukuManager>()

    var shizukuAvailable by remember { mutableStateOf(false) }
    var dhizukuAvailable by remember { mutableStateOf(false) }
    var selectedInstaller by rememberSaveable { mutableStateOf(currentInstaller) }

    LaunchedEffect(Unit) {
        shizukuAvailable = shizuku.shizukuAvailable()
        dhizukuAvailable = dhizuku.dhizukuAvailable()
    }

    // Check if selected installer is usable and ask for permissions when necessary
    LaunchedEffect(selectedInstaller) {
        when (selectedInstaller) {
            InstallerSetting.PackageInstaller -> {
                // Once the Google sideloading block is in place,
                // check whether it is applicable to the device, and if so then it needs
                // to be inaccessible. (Disable button)
            }

            InstallerSetting.Root -> {
                val shell = Shell.getShell()
                if (!shell.isRoot) {
                    shell.waitAndClose()
                    Shell.getShell()
                }

                if (Shell.isAppGrantedRoot() != true) {
                    context.showToast(R.string.permissions_root_denied)
                    selectedInstaller = InstallerSetting.PackageInstaller
                }
            }

            InstallerSetting.Intent -> {
                // We don't know whether this device supports this method until we try.
            }

            InstallerSetting.Shizuku -> {
                if (!shizuku.requestPermissions()) {
                    selectedInstaller = InstallerSetting.PackageInstaller
                }
            }

            InstallerSetting.Dhizuku -> {
                if (!dhizuku.requestPermissions()) {
                    selectedInstaller = InstallerSetting.PackageInstaller
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_apk_install),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text(stringResource(R.string.setting_installer)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (installer in InstallerSetting.entries) key(installer) {
                    InstallerItem(
                        installer = installer,
                        selected = installer == selectedInstaller,
                        enabled = when (installer) {
                            InstallerSetting.PackageInstaller -> true
                            InstallerSetting.Root -> true
                            InstallerSetting.Intent -> true
                            InstallerSetting.Shizuku -> shizukuAvailable
                            InstallerSetting.Dhizuku -> dhizukuAvailable
                        },
                        onClick = { selectedInstaller = installer },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedInstaller)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
    )
}

@Composable
private fun InstallerItem(
    installer: InstallerSetting,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember(::MutableInteractionSource)

    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = Modifier.Companion
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
            ) {
                Icon(
                    painter = installer.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                )
                Text(
                    text = installer.title(),
                    style = MaterialTheme.typography.labelLarge
                        .copy(fontSize = 14.sp)
                )
            }
            Text(
                text = installer.description(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                ),
            )
        }

        Spacer(Modifier.Companion.weight(0.05f, true))

        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
        )
    }
}
