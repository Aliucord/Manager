package com.aliucord.manager.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.aliucord.manager.R
import com.xinto.taxi.Destination
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
sealed interface AppDestination : Destination {
    @Parcelize
    object Home : AppDestination

    @Parcelize
    object Plugins : AppDestination

    @Parcelize
    object Install : AppDestination

    @Parcelize
    object Store : AppDestination

    @Parcelize
    object About : AppDestination

    @Parcelize
    object Settings : AppDestination
}

@Parcelize
enum class HomeDestination(
    val selectedIcon: @RawValue ImageVector,
    val unselectedIcon: @RawValue ImageVector,
    @StringRes val label: Int
) : Destination {
    HOME(Icons.Default.Home, Icons.Outlined.Home, R.string.home),
    PLUGINS(Icons.Default.Extension, Icons.Outlined.Extension, R.string.plugins)
}
