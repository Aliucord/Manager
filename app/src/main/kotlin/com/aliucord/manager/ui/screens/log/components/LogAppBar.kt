package com.aliucord.manager.ui.screens.log.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton

@Composable
fun LogAppBar(
    onExportLog: () -> Unit,
    onShareLog: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.log_title)) },
        navigationIcon = { BackButton() },
        actions = {
            IconButton(onClick = onExportLog) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.log_action_export),
                )
            }
            IconButton(onClick = onShareLog) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.log_action_share),
                )
            }
        },
    )
}
