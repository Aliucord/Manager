package com.aliucord.manager.ui.screens.install.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.install.InstallScreenState

@Composable
fun InstalledButtonRow(
    state: InstallScreenState,
    onRetry: () -> Unit,
    onLaunch: () -> Unit,
    onClearCache: () -> Unit,
    onSaveLog: () -> Unit,
    onShareLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilledTonalButton(onClick = onClearCache) {
                Text(stringResource(R.string.setting_clear_cache))
            }

            Spacer(Modifier.weight(1f, true))

            // OutlinedButton(onClick = onSaveLog) {
            //     Text(stringResource(R.string.installer_save_file))
            // }
        }
    }
}
