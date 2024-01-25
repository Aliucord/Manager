package com.aliucord.manager.ui.util.paddings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.LayoutDirection

/**
 * Add the values of two [PaddingValues] together.
 */
fun PaddingValues.add(values: PaddingValues): PaddingValues =
    CompositePaddingValues(this, values)

@Stable
private class CompositePaddingValues(
    private val valuesA: PaddingValues,
    private val valuesB: PaddingValues,
) : PaddingValues {
    override fun calculateBottomPadding() =
        valuesA.calculateBottomPadding() + valuesB.calculateBottomPadding()

    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        valuesA.calculateLeftPadding(layoutDirection) + valuesB.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        valuesA.calculateRightPadding(layoutDirection) + valuesB.calculateRightPadding(layoutDirection)

    override fun calculateTopPadding() =
        valuesA.calculateTopPadding() + valuesB.calculateTopPadding()
}
