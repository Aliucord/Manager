package com.aliucord.manager.ui.previews.dialogs

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.components.Theme
import com.aliucord.manager.ui.screens.settings.components.ThemeDialog

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ThemeDialogPreview() {
    val (theme, setTheme) = remember { mutableStateOf(Theme.SYSTEM) }

    ManagerTheme {
        ThemeDialog(
            currentTheme = theme,
            onDismissRequest = {},
            onConfirm = setTheme,
        )
    }
}
