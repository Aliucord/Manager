package com.aliucord.manager.ui.previews.screens.about

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.screens.about.AboutScreenContent
import com.aliucord.manager.ui.screens.about.AboutScreenState
import com.aliucord.manager.ui.theme.ManagerTheme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun AboutScreenFailedPreview() {
    ManagerTheme {
        AboutScreenContent(
            state = remember { mutableStateOf(AboutScreenState.Failure) },
        )
    }
}
