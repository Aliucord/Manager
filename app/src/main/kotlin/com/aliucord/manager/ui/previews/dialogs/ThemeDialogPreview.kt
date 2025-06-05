package com.aliucord.manager.ui.previews.dialogs

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.screens.settings.components.ThemeDialog
import com.aliucord.manager.ui.theme.ManagerTheme
import com.aliucord.manager.ui.theme.Theme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ThemeDialogPreview() {
    val (theme, setTheme) = remember { mutableStateOf(Theme.System) }

    ManagerTheme {
        ThemeDialog(
            currentTheme = theme,
            onDismissRequest = {},
            onConfirm = setTheme,
        )
    }
}
