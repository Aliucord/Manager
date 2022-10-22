/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import android.os.Parcelable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.ui.dialog.DiscordType
import com.aliucord.manager.ui.dialog.DownloadMethod
import com.aliucord.manager.ui.viewmodel.InstallViewModel
import kotlinx.parcelize.Parcelize
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Parcelize
data class InstallData(
    val downloadMethod: DownloadMethod,
    val discordType: DiscordType,
    var baseApk: String? = null,
    var splits: List<String>? = null
) : Parcelable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    installData: InstallData,
    onBackClick: () -> Unit,
    viewModel: InstallViewModel = getViewModel(parameters = { parametersOf(installData) })
) {
    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)

    if (navigateMain) onBackClick()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installer") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                item {
                    Text(
                        text = viewModel.log,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
