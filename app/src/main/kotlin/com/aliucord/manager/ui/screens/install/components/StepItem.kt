package com.aliucord.manager.ui.screens.install.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliucord.manager.installer.steps.base.Step

@Composable
fun StepItem(
    step: Step,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        StepStatusIcon(step.state, size = 18.dp)

        Text(
            text = stringResource(step.localizedName),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, true),
        )

        // TODO: live step duration counter
        if (step.state.isFinished) {
            Text(
                text = "%.2fs".format(step.durationMs / 1000f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}
