/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.ui.screens.destinations.HomeScreenDestination
import com.aliucord.manager.ui.viewmodels.InstallViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun InstallerScreen(navigator: DestinationsNavigator, apk: File?) {
    val viewModel: InstallViewModel = viewModel()

    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)
    if (navigateMain) {
        navigator.navigate(HomeScreenDestination)
    }

    LaunchedEffect(Unit) {
        viewModel.startInstallation(apk)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        stickyHeader {
            LinearProgressIndicator(
                modifier = Modifier.fillParentMaxWidth()
            )
        }
        items(viewModel.logs) { log ->
            Text(
                text = log,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
