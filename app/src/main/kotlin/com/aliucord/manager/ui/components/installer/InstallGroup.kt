package com.aliucord.manager.ui.components.installer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.aliucord.manager.R
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.floor

enum class InstallStatus {
    ONGOING,
    SUCCESSFUL,
    UNSUCCESSFUL,
    QUEUED
}

@Stable
class InstallStepData(
    val nameResId: Int,
    status: InstallStatus,
    duration: Float = 0f,
    cached: Boolean = false,
) {
    var status by mutableStateOf(status)
    var duration by mutableStateOf(duration)
    var cached by mutableStateOf(cached)
}

@Composable
fun InstallGroup(
    name: String,
    isCurrent: Boolean,
    subSteps: ImmutableList<InstallStepData>,
    onClick: () -> Unit,
) {
    val status = when {
        subSteps.all { it.status == InstallStatus.QUEUED } ->
            InstallStatus.QUEUED

        subSteps.all { it.status == InstallStatus.SUCCESSFUL } ->
            InstallStatus.SUCCESSFUL

        subSteps.any { it.status == InstallStatus.ONGOING } ->
            InstallStatus.ONGOING

        else -> InstallStatus.UNSUCCESSFUL
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .run {
                if (isCurrent) {
                    background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                } else this
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clickable(true, onClick = onClick)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StepIcon(status, 24.dp)

            Text(text = name)

            Spacer(modifier = Modifier.weight(1f))

            if (status != InstallStatus.ONGOING && status != InstallStatus.QUEUED) Text(
                "%.2fs".format(subSteps.map { it.duration }.sum()),
                style = MaterialTheme.typography.labelMedium
            )

            if (isCurrent) {
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

        AnimatedVisibility(visible = isCurrent) {
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
                        StepIcon(it.status, size = 18.dp)

                        Text(
                            text = stringResource(it.nameResId),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, true),
                        )

                        if (it.status != InstallStatus.ONGOING && it.status != InstallStatus.QUEUED) {
                            if (it.cached) {
                                val style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = stringResource(R.string.installer_cached),
                                    style = style,
                                    maxLines = 1,
                                )
                            }

                            Text(
                                text = "%.2fs".format(it.duration),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIcon(status: InstallStatus, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)
    val context = LocalContext.current

    when (status) {
        InstallStatus.ONGOING -> CircularProgressIndicator(
            strokeWidth = strokeWidth,
            modifier = Modifier
                .size(size)
                .semantics {
                    contentDescription = context.getString(R.string.status_ongoing)
                }
        )

        InstallStatus.SUCCESSFUL -> Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = stringResource(R.string.status_success),
            tint = Color(0xFF59B463),
            modifier = Modifier.size(size)
        )

        InstallStatus.UNSUCCESSFUL -> Icon(
            painter = painterResource(R.drawable.ic_canceled),
            contentDescription = stringResource(R.string.status_failed),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(size)
        )

        InstallStatus.QUEUED -> Icon(
            painter = painterResource(R.drawable.ic_circle),
            contentDescription = stringResource(R.string.status_queued),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
            modifier = Modifier.size(size)
        )
    }
}
