package com.aliucord.manager.ui.previews.dialogs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.components.dialogs.InstallerAbortDialog
import com.aliucord.manager.ui.theme.ManagerTheme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun InstallerAbortDialogPreview() {
    ManagerTheme {
        InstallerAbortDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
