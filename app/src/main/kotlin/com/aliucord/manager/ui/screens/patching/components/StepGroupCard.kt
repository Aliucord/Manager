package com.aliucord.manager.ui.screens.patching.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun StepGroupCard(
    name: String,
    subSteps: ImmutableList<Step>,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupState by remember(subSteps) {
        derivedStateOf {
            when {
                // If all steps are pending then show pending
                subSteps.all { it.state == StepState.Pending } -> StepState.Pending
                // If any step has finished with an error then default to error
                subSteps.any { it.state == StepState.Error } -> StepState.Error
                // If all steps have finished as Skipped/Success then show success
                subSteps.all { it.state.isFinished } -> StepState.Success

                else -> StepState.Running
            }
        }
    }

    val totalSeconds = remember(groupState.isFinished) {
        if (!groupState.isFinished) {
            0f
        } else {
            subSteps
                .sumOf { step -> step.getDuration() }
                .div(1000f)
        }
    }

    LaunchedEffect(groupState) {
        if (groupState == StepState.Running)
            onExpand()
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .clickable(true, onClick = onExpand)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            StepStateIcon(
                state = groupState,
                size = 24.dp,
            )

            Text(text = name)

            Spacer(modifier = Modifier.weight(1f))

            TimeElapsed(
                enabled = groupState.isFinished,
                seconds = totalSeconds,
            )

            if (isExpanded) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_up_small),
                    contentDescription = stringResource(R.string.action_collapse)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down_small),
                    contentDescription = stringResource(R.string.action_expand)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(start = 4.dp)
            ) {
                for (step in subSteps) key(step) {
                    StepItem(step)
                }
            }
        }
    }
}
