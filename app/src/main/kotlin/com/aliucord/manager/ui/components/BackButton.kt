package com.aliucord.manager.ui.components

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.home.HomeScreen

/**
 * Standalone back button for interacting with the current navigator.
 */
@Composable
fun BackButton() {
    // Support rendering in @Preview
    val context = if (LocalInspectionMode.current) null else LocalActivity.current
    val navigator = LocalNavigator.current

    IconButton(
        onClick = { if (context != null) navigator?.back(context) },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.navigation_back),
        )
    }
}

/**
 * Custom back logic for handling special screens differently.
 * If on Home, Landing, or Debug then exit immediately otherwise go back one.
 */
fun Navigator.back(currentActivity: Activity?) {
    val top = this.lastItemOrNull ?: return
    val stackSize = this.items.size

    if (stackSize > 1) {
        pop()
    } else if (top is HomeScreen) {
        currentActivity?.finish()
    } else {
        replaceAll(HomeScreen())
    }
}
