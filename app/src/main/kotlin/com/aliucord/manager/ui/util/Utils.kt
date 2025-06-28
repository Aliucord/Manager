@file:Suppress("NOTHING_TO_INLINE")

package com.aliucord.manager.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

@Stable
inline fun Modifier.mirrorVertically(): Modifier =
    scale(scaleX = -1f, scaleY = 1f)

/**
 * Allow using compose [Color] in [androidx.compose.runtime.saveable.rememberSaveable]
 */
val ColorSaver = Saver<Color, Long>(
    save = { it.value.toLong() },
    restore = { Color(it.toULong()) },
)
