package com.aliucord.manager.ui.util.paddings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

typealias PaddingValuesSides = WindowInsetsSides

/**
 * Remove particular side values from [PaddingValues].
 */
fun PaddingValues.exclude(sides: PaddingValuesSides): PaddingValues =
    ExcludePaddingValues(this, sides)

@Stable
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
private class ExcludePaddingValues(
    private val values: PaddingValues,
    private val excludeSides: PaddingValuesSides,
) : PaddingValues {
    override fun calculateBottomPadding(): Dp {
        return if (!excludeSides.hasAny(PaddingValuesSides.Bottom)) {
            values.calculateBottomPadding()
        } else {
            Dp.Hairline
        }
    }

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        return if (!excludeSides.hasAny(PaddingValuesSides.Left)) {
            values.calculateLeftPadding(layoutDirection)
        } else {
            Dp.Hairline
        }
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        return if (!excludeSides.hasAny(PaddingValuesSides.Right)) {
            values.calculateRightPadding(layoutDirection)
        } else {
            Dp.Hairline
        }
    }

    override fun calculateTopPadding(): Dp {
        return if (!excludeSides.hasAny(PaddingValuesSides.Top)) {
            values.calculateTopPadding()
        } else {
            Dp.Hairline
        }
    }
}
