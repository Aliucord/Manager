package com.aliucord.manager.ui.screens.iconopts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun TransparencyGrid(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .drawBehind {
                drawRect(color = Color.White)

                val squareSize = 20
                val paintableWidth = size.width.toInt()
                val paintableHeight = size.height.toInt()

                for (x in 0..paintableWidth step (2 * squareSize)) {
                    for (y in 0..paintableHeight step squareSize) {
                        val shouldAlternate = (y / squareSize).mod(2) != 0
                        val xOffset = x.toFloat() + if (shouldAlternate) squareSize.toFloat() else 0f
                        val yOffset = y.toFloat()

                        drawRect(
                            color = Color.LightGray,
                            topLeft = Offset(xOffset, yOffset),
                            size = Size(squareSize.toFloat(), squareSize.toFloat()),
                        )
                    }
                }
            }
    )
}
