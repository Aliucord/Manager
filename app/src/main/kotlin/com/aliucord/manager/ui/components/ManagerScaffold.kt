/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components

import android.Manifest
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.aliucord.manager.ui.screens.NavGraphs
import com.google.accompanist.permissions.*
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.rememberNavHostEngine

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun ManagerScaffold() {
    val navController = rememberNavController()

    Scaffold(
        topBar = { AppBar(navController) }
    ) { paddingValues ->
        val storagePermissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)

        PermissionRequired(
            permissionState = storagePermissionState,
            permissionNotAvailableContent = { GrantPermission(storagePermissionState) },
            permissionNotGrantedContent = { GrantPermission(storagePermissionState) }
        ) {
            DestinationsNavHost(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                navGraph = NavGraphs.root,
                navController = navController,
                engine = rememberNavHostEngine()
            )
        }
    }
}
