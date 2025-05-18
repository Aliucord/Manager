package com.aliucord.manager.ui.screens.logs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.R
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.ui.screens.logs.LogEntry
import com.aliucord.manager.ui.screens.patching.components.StepStateIcon
import com.aliucord.manager.ui.screens.patching.components.TimeElapsed

@Composable
fun LogEntryCard(
    data: LogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error

    ElevatedCard(
        shape = RectangleShape,
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(topStart = 6.dp, 12.0.dp, bottomStart = 6.dp, bottomEnd = 12.0.dp))
            .drawWithCache {
                val color = when (data.isError) {
                    true -> errorColor
                    false -> Color(0xFF59B463)
                }

                onDrawWithContent {
                    drawContent()
                    drawRect(
                        color = color,
                        alpha = .8f,
                        topLeft = Offset.Zero,
                        size = Size(4.dp.toPx(), size.height),
                    )
                }
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 18.dp, horizontal = 22.dp),
        ) {
            StepStateIcon(
                state = if (data.isError) StepState.Error else StepState.Success,
                size = 24.dp,
            )

            Column {
                Text(
                    text = when (data.isError) {
                        true -> stringResource(R.string.status_failed)
                        false -> stringResource(R.string.status_success)
                    },
                )

                Text(
                    text = data.installDate,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.alpha(.6f),
                )
            }

            Spacer(Modifier.weight(1f, fill = true))

            TimeElapsed(
                seconds = data.durationSecs,
                modifier = Modifier.alpha(.9f),
            )
        }

        if (data.stacktracePreview != null) {
            Column(
                modifier = Modifier
                    .padding(start = 26.dp, end = 20.dp, bottom = 18.dp)
                    // https://stackoverflow.com/a/76270310/13964629
                    .graphicsLayer(
                        alpha = .95f,
                        compositingStrategy = CompositingStrategy.Offscreen,
                    )
                    .drawWithContent {
                        val colors = listOf(Color.Black, Color.Black, Color.Transparent)
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(colors),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            ) {
                // The stacktrace is separated into multiple Text elements because ellipsis is not supported per-line
                for (line in data.stacktracePreview) key(line) {
                    Text(
                        text = line,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Companion.Monospace,
                    )
                }
            }
        }
    }
}
