package com.aliucord.manager.ui.screens.install.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.customColors
import com.aliucord.manager.util.isIgnoringBatteryOptimizations

@Composable
fun MinimizationWarning(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hide = remember(context) { context.isIgnoringBatteryOptimizations() }

    if (hide) return

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 4.dp,
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.customColors.warning,
                shape = MaterialTheme.shapes.medium,
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.customColors.warningContainer)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                tint = MaterialTheme.customColors.onWarningContainer,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )

            Text(
                text = stringResource(R.string.installer_minimization_warning),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
