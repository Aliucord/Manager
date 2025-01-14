@file:Suppress("NOTHING_TO_INLINE")

package com.aliucord.manager.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import kotlinx.collections.immutable.ImmutableList

/**
 * This formats and styles string resources into an [AnnotatedString].
 * By putting `§annotation_name§target string blah blah blah§` into the resource string it can be translated
 * (assuming `annotation_name` is not changed between translations),
 * when rendering [annotationHandler] will be called on each `annotation_name` and expected to return a target style on it.
 */
@Composable
inline fun annotatingStringResource(
    @StringRes id: Int,
    args: ImmutableList<Any>,
    crossinline annotationHandler: @Composable (annotationName: String) -> SpanStyle?,
): AnnotatedString {
    val string = stringResource(id, *args.toTypedArray())

    val markerIndexes = string
        .mapIndexedNotNull { index, elem -> index.takeIf { elem == '§' } }
        .takeIf { it.isNotEmpty() && it.size % 3 == 0 }
        ?: return AnnotatedString(string)

    return buildAnnotatedString {
        var lastIndex = 0
        var offset = 0

        for ((nameStart, nameEnd, valueEnd) in markerIndexes.chunked(3)) {
            append(string.substring(lastIndex, nameStart))
            append(string.substring(nameEnd + 1, valueEnd))

            val annotationName = string.substring(nameStart + 1, nameEnd)
            val newValueStart = nameEnd - annotationName.length - offset - 1
            val newValueEnd = valueEnd - annotationName.length - offset - 2

            addStringAnnotation(
                tag = annotationName,
                annotation = annotationName,
                start = newValueStart,
                end = newValueEnd,
            )

            annotationHandler(annotationName)?.let {
                addStyle(it, newValueStart, newValueEnd)
            }

            lastIndex = valueEnd + 1
            offset += 3 + annotationName.length
        }

        append(string.substring(lastIndex))
    }
}

@Stable
inline fun Modifier.mirrorVertically(): Modifier =
    scale(scaleX = -1f, scaleY = 1f)
