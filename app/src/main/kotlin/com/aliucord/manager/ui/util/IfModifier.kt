@file:Suppress("unused")

package com.aliucord.manager.ui.util

import androidx.compose.ui.Modifier

/**
 * Apply additional modifiers if [value] is not null.
 */
inline fun <T> Modifier.thenIf(value: T?, block: Modifier.(T) -> Modifier): Modifier =
    value?.let { block(it) } ?: this

/**
 * Apply additional modifiers if [predicate] is true.
 */
inline fun Modifier.thenIf(predicate: Boolean, block: Modifier.() -> Modifier): Modifier {
    return if (predicate) {
        block()
    } else {
        this
    }
}
