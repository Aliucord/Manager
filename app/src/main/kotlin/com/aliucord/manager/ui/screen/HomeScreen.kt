/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliucord.manager.ui.component.home.CommitList
import com.aliucord.manager.ui.component.home.InfoCard
import com.aliucord.manager.ui.dialog.*
import com.aliucord.manager.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = getViewModel(),
    onClickInstall: (InstallData) -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }

    if (showOptionsDialog) {
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            onClickInstall(
                InstallData(
                    DownloadMethod.SELECT,
                    DiscordType.REACT_NATIVE
                )
            )
        }

        InstallerDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = { data ->
                when (data.downloadMethod) {
                    DownloadMethod.SELECT -> filePicker.launch(arrayOf("application/octet-stream"))
                    DownloadMethod.DOWNLOAD -> onClickInstall(data)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard(
            packageName = viewModel.preferences.packageName,
            supportedVersion = viewModel.supportedVersion,
            supportedVersionType = viewModel.supportedVersionType,
            currentVersion = viewModel.installedVersion,
            onDownloadClick = {
                if (viewModel.preferences.devMode) {
                    showOptionsDialog = true
                } else {
                    onClickInstall(
                        InstallData(
                            DownloadMethod.DOWNLOAD,
                            DiscordType.REACT_NATIVE
                        )
                    )
                }
            },
            onLaunchClick = viewModel::launchAliucord,
            onUninstallClick = viewModel::uninstallAliucord
        )

        CommitList(
            commits = viewModel.commits,
            onRetry = { viewModel.fetchSupportedVersion() }
        )
    }
}
