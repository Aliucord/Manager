package com.aliucord.manager.ui.previews.screens.home

import android.content.res.Configuration
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.screens.home.HomeScreenNoneContent
import com.aliucord.manager.ui.screens.home.components.HomeAppBar
import com.aliucord.manager.ui.theme.ManagerTheme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun HomeScreenNonePreview() {
    ManagerTheme {
        Scaffold(
            topBar = { HomeAppBar() },
        ) { padding ->
            HomeScreenNoneContent(
                padding = padding,
                onClickInstall = {},
            )
        }
    }
}
