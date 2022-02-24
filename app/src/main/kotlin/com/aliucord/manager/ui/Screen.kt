/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import com.aliucord.manager.R

@ExperimentalFoundationApi
sealed class Screen(val route: String, @StringRes val displayName: Int) {
    companion object {
        val screens by lazy { listOf(Home, About, Settings, Commits, Installer, Store) }
    }

    object Home : Screen("home", R.string.app_name)
    object About : Screen("about", R.string.about)
    object Settings : Screen("settings", R.string.settings)
    object Commits : Screen("commits", R.string.commits)
    object Installer : Screen("installer", R.string.installer)
    object Store : Screen("store", R.string.store)
}