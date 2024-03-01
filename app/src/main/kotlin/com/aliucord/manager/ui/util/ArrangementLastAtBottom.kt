package com.aliucord.manager.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.*
import kotlin.math.min

/**
 * Arranges all the elements with [spacing] except for the last element which is
 * spaced to the very bottom of the viewable component.
 *
 * ref: https://stackoverflow.com/a/69196765/13964629
 */
@Immutable
class ArrangementLastAtBottom(
    override val spacing: Dp = 0.dp,
) : Arrangement.Vertical {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return

        val spacingPx = spacing.roundToPx()
        var occupied = 0
        var lastSpace = 0

        sizes.forEachIndexed { index, size ->
            if (index == sizes.lastIndex) {
                outPositions[index] = totalSize - size
            } else {
                outPositions[index] = min(occupied, totalSize - size)
            }

            lastSpace = min(spacingPx, totalSize - outPositions[index] - size)
            occupied = outPositions[index] + size + lastSpace
        }

        occupied -= lastSpace
    }
}

/**
 * Arranges all the elements vertically with [space] except for the last element
 * which is drawn at the very bottom of the viewable bounds of the component.
 */
@Stable
@Suppress("UnusedReceiverParameter")
fun Arrangement.spacedByLastAtBottom(space: Dp) =
    ArrangementLastAtBottom(space)
