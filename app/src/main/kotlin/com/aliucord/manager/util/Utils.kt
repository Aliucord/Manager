package com.aliucord.manager.util

import kotlin.math.pow
import kotlin.math.truncate

/**
 * Truncates this value to a specific number of [decimals] digits.
 */
fun Double.toPrecision(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return truncate(this * multiplier) / multiplier
}
