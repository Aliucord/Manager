package com.aliucord.manager.ui.util.paddings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Remove particular side values from [PaddingValues].
 */
fun PaddingValues.exclude(sides: PaddingValuesSides): PaddingValues =
    ExcludePaddingValues(this, sides)

@Stable
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

/**
 * Copy of [WindowInsetsSides] with the necessary methods exposed publicly.
 */
@Suppress("unused")
@JvmInline
value class PaddingValuesSides private constructor(private val value: Int) {
    /**
     * Returns a [PaddingValuesSides] containing sides defied in [sides] and the
     * sides in `this`.
     */
    operator fun plus(sides: PaddingValuesSides): PaddingValuesSides =
        PaddingValuesSides(value or sides.value)

    internal fun hasAny(sides: PaddingValuesSides): Boolean =
        (value and sides.value) != 0

    override fun toString(): String = "PaddingValuesSides(${valueToString()})"

    private fun valueToString(): String = buildString {
        fun appendPlus(text: String) {
            if (isNotEmpty()) append('+')
            append(text)
        }

        if (value and Start.value == Start.value) appendPlus("Start")
        if (value and Left.value == Left.value) appendPlus("Left")
        if (value and Top.value == Top.value) appendPlus("Top")
        if (value and End.value == End.value) appendPlus("End")
        if (value and Right.value == Right.value) appendPlus("Right")
        if (value and Bottom.value == Bottom.value) appendPlus("Bottom")
    }

    companion object {
        internal val AllowLeftInLtr = PaddingValuesSides(1 shl 3)
        internal val AllowRightInLtr = PaddingValuesSides(1 shl 2)
        internal val AllowLeftInRtl = PaddingValuesSides(1 shl 1)
        internal val AllowRightInRtl = PaddingValuesSides(1 shl 0)

        /**
         * Indicates a start side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [Start]
         * is the left side. If [LayoutDirection.Rtl], [Start] is the right side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val Start = AllowLeftInLtr + AllowRightInRtl

        /**
         * Indicates an end side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [End]
         * is the right side. If [LayoutDirection.Rtl], [End] is the left side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val End = AllowRightInLtr + AllowLeftInRtl

        /**
         * Indicates the top side.
         */
        val Top = PaddingValuesSides(1 shl 4)

        /**
         * Indicates the bottom side.
         */
        val Bottom = PaddingValuesSides(1 shl 5)

        /**
         * Indicates a left side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Left = AllowLeftInLtr + AllowLeftInRtl

        /**
         * Indicates a right side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Right = AllowRightInLtr + AllowRightInRtl

        /**
         * Indicates a horizontal sides. This is a combination of
         * [Left] and [Right] sides, or [Start] and [End] sides.
         */
        val Horizontal = Left + Right

        /**
         * Indicates a [Top] and [Bottom] sides.
         */
        val Vertical = Top + Bottom
    }
}
