/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.HomeScreen
import com.aliucord.manager.ui.screens.SettingsScreen

sealed class Screen(
    val route: String,
    @StringRes val displayName: Int,
) {
    companion object {
        // NOTE: without lazy it doesn't work, kotlin moment https://youtrack.jetbrains.com/issue/KT-8970
        val SCREENS by lazy { listOf(Home, Settings) }
    }

    object Home : Screen(
        route = "home",
        displayName = R.string.app_name,
    )

    object Settings : Screen(
        route = "settings",
        displayName = R.string.settings,
    )
}
