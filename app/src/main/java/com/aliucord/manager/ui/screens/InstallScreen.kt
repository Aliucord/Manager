package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

//TODO finish this
@Composable
fun InstallScreen(
    navController: NavController
) {
    navController.enableOnBackPressed(false)

    var progress by remember { mutableStateOf(0f) }

    val logs = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress)
    }
}