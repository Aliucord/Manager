package com.aliucord.manager.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

// Based on https://gist.github.com/XFY9326/2067efcc3c5899557cc6a334d76a92c8

fun Modifier.horizontalScrollbar(
    scrollState: ScrollState,
    scrollBarHeight: Dp = 4.dp,
    minScrollBarWidth: Dp = 5.dp,
    scrollBarColor: Color = Color.White.copy(alpha = .4f),
    cornerRadius: Dp = 2.dp,
): Modifier = composed {
    drawWithContent {
        drawContent()

        if (scrollState.maxValue <= 0) return@drawWithContent

        val visibleWidth: Float = this.size.width - scrollState.maxValue
        val scrollBarWidth: Float = max(visibleWidth * (visibleWidth / this.size.width), minScrollBarWidth.toPx())
        val scrollPercent: Float = scrollState.value.toFloat() / scrollState.maxValue
        val scrollBarOffsetX: Float = scrollState.value + (visibleWidth - scrollBarWidth) * scrollPercent

        drawRoundRect(
            color = scrollBarColor,
            topLeft = Offset(scrollBarOffsetX, this.size.height - scrollBarHeight.toPx()),
            size = Size(scrollBarWidth, scrollBarHeight.toPx()),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
}
