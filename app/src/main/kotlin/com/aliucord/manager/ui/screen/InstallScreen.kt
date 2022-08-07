/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.ui.viewmodel.InstallViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    onClickBack: () -> Unit
) {
    val viewModel: InstallViewModel = viewModel()

    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)
    if (navigateMain) onClickBack

    LaunchedEffect(Unit) {
        viewModel.startInstallation()
    }


    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Installer") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            stickyHeader {
                LinearProgressIndicator(
                    modifier = Modifier.fillParentMaxWidth()
                )
            }
            item {
                Text(
                    text = viewModel.log,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
