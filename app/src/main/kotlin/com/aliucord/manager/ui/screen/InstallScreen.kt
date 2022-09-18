/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.viewmodel.InstallViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    viewModel: InstallViewModel = getViewModel(),
    onClickBack: () -> Unit
) {
    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)

    if (navigateMain) onClickBack()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installer") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.back)
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
        ) {
            stickyHeader {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            item {
                Text(
                    text = viewModel.log,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}
