package com.aliucord.manager.ui.navigation

import android.app.Activity
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aliucord.manager.R
import com.aliucord.manager.ui.screen.InstallData
import dev.olshevski.navigation.reimagined.*
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class BaseScreenDestination(
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
    @StringRes val label: Int
) : AppDestination

@Parcelize
sealed interface AppDestination : Parcelable {
    @Parcelize
    object Home : BaseScreenDestination(
        R.drawable.ic_home,
        R.drawable.ic_home, // TODO: get ic_home outline variant
        R.string.navigation_home
    )

    @Parcelize
    object Plugins : BaseScreenDestination(
        R.drawable.ic_extension,
        R.drawable.ic_extension_outlined,
        R.string.plugins_title
    )

    @Parcelize
    object Settings : BaseScreenDestination(
        R.drawable.ic_settings,
        R.drawable.ic_settings_outlined,
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
