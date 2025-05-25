package com.aliucord.manager.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import com.aliucord.manager.R
import com.aliucord.manager.util.back

/**
 * Standalone back button for interacting with the current navigator.
 */
@Composable
fun BackButton() {
    val navigator = LocalNavigator.current
    val activity = LocalActivity.current

    IconButton(
        onClick = {
            navigator?.back(activity)
        },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.navigation_back),
        )
    }
}
