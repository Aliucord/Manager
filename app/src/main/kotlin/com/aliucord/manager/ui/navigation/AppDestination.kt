package com.aliucord.manager.ui.navigation

import android.app.Activity
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.aliucord.manager.R
import com.aliucord.manager.ui.screen.InstallData
import dev.olshevski.navigation.reimagined.*
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
sealed class BaseScreenDestination(
    val selectedIcon: @RawValue ImageVector,
    val unselectedIcon: @RawValue ImageVector,
    @StringRes val label: Int
) : AppDestination

@Parcelize
sealed interface AppDestination : Parcelable {
    @Parcelize
    object Home : BaseScreenDestination(
        Icons.Default.Home,
        Icons.Outlined.Home,
        R.string.navigation_home
    )

    @Parcelize
    object Plugins : BaseScreenDestination(
        Icons.Default.Extension,
        Icons.Outlined.Extension,
        R.string.plugins_title
    )

    @Parcelize
    object Settings : BaseScreenDestination(
        Icons.Default.Settings,
        Icons.Outlined.Settings,
        R.string.navigation_settings
    )

    @Parcelize
    data class Install(val installData: InstallData) : AppDestination

    @Parcelize
    object About : AppDestination
}

context(Activity)
fun NavController<AppDestination>.back() {
    val topDest = backstack.entries.lastOrNull()?.destination

    if (topDest == AppDestination.Home) {
        finish()
    } else if (topDest is BaseScreenDestination) {
        replaceAll(AppDestination.Home)
    } else if (backstack.entries.size > 1) {
        pop()
    } else {
        replaceAll(AppDestination.Home)
    }
}
