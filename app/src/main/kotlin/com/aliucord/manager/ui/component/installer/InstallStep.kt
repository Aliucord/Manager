package com.aliucord.manager.ui.component.installer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import kotlin.math.floor

enum class Status {
    ONGOING,
    SUCCESSFUL,
    UNSUCCESSFUL,
    QUEUED
}

data class Step(
    var text: String,
    var status: Status,
    var duration: Float = 0f,
    var cached: Boolean = false
)

@Composable
fun InstallStep(
    name: String,
    isCurrent: Boolean,
    subSteps: List<Step>
) {
    var collapsed by mutableStateOf(!isCurrent)

    val status =
        if(subSteps.all { it.status == Status.QUEUED })
            Status.QUEUED
        else if(subSteps.any { it.status == Status.UNSUCCESSFUL })
            Status.UNSUCCESSFUL
        else if(subSteps.any { it.status == Status.ONGOING })
            Status.ONGOING
        else Status.SUCCESSFUL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .run {
                if(isCurrent) {
                    background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                } else this
            }
    ) {
        Row(
           verticalAlignment = Alignment.CenterVertically,
           horizontalArrangement = Arrangement.spacedBy(12.dp),
           modifier = Modifier
               .clickable {
                   collapsed = !collapsed
               }
               .fillMaxWidth()
               .padding(16.dp)
        ) {

            StepIcon(status, 24.dp)

            Text(text = name)

            Spacer(modifier = Modifier.weight(1f))

            if(status != Status.ONGOING && status != Status.QUEUED) Text(
                "%.2fs".format(subSteps.map { it.duration }.sum()),
                style = MaterialTheme.typography.labelMedium
            )

            val (icon, contentDescription) = if(collapsed) {
                Icons.Filled.KeyboardArrowDown to "Expand"
            } else {
                Icons.Filled.KeyboardArrowUp to "Collapse"
            }

            Icon(
                icon,
                contentDescription
            )
        }

        AnimatedVisibility(visible = !collapsed) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(start = 4.dp)
            ) {
                subSteps.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StepIcon(it.status, size = 16.dp)

                        Text(text = it.text, style = MaterialTheme.typography.labelLarge)

                        if(it.status != Status.ONGOING && it.status != Status.QUEUED) {
                            Spacer(modifier = Modifier.weight(1f))
                            if(it.cached) Text(stringResource(R.string.installer_cached), style = MaterialTheme.typography.labelSmall)
                            Text(text = " %.2fs".format(it.duration), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIcon(status: Status, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)

    when(status) {
        Status.ONGOING ->  CircularProgressIndicator(
            strokeWidth = strokeWidth,
            modifier = Modifier.size(size)
        )
        Status.SUCCESSFUL -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.installer_completed),
            tint = Color(0xFF59B463),
            modifier = Modifier.size(size)
        )
        Status.UNSUCCESSFUL -> Icon(
            Icons.Filled.Cancel,
            contentDescription = "Failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(size)
        )
        Status.QUEUED -> Icon(
            Icons.Outlined.Circle,
            contentDescription = "Waiting",
            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
            modifier = Modifier.size(size)
        )
    }
}
