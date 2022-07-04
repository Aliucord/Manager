/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.ui.screen.destinations.HomeScreenDestination
import com.aliucord.manager.ui.viewmodel.InstallViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun InstallerScreen(navigator: DestinationsNavigator) {
    val viewModel: InstallViewModel = viewModel()

    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)
    if (navigateMain) navigator.navigate(HomeScreenDestination)

    LaunchedEffect(Unit) {
        viewModel.startInstallation()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
