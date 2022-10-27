package com.aliucord.manager.ui.util

import android.content.*
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*

context(AnnotatedString.Builder)
    inline fun <T> Iterable<T>.joinToAnnotatedString(
    separator: AnnotatedString.Builder.() -> Unit = { append(", ") },
    prefix: AnnotatedString.Builder.() -> Unit = {},
    postfix: AnnotatedString.Builder.() -> Unit = {},
    transform: (AnnotatedString.Builder.(T) -> Unit) = {
        when (it) {
            is Char -> append(it)
            else -> append(it.toString())
        }
    }
) {
    prefix(this@Builder)
    for ((count, element) in this@joinToAnnotatedString.withIndex()) {
        if (count + 1 > 1) separator(this@Builder)
        transform(this@Builder, element)
    }
    postfix(this@Builder)
}

/**
 * This formats and styles string resources into an [AnnotatedString].
 * By putting `§annotation_name§target string blah blah blah§` into the resource string it can be translated
 * (assuming `annotation_name` is not changed between translations),
 * when rendering [annotationHandler] will be called on each `annotation_name` and expected to return a target style on it.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun annotatingStringResource(
    @StringRes id: Int,
    vararg args: Any,
    crossinline annotationHandler: @Composable (annotationName: String) -> SpanStyle?
): AnnotatedString {
    val string = stringResource(id, *args)

    // TODO: figure out a way to put this in remember {} while still having @Composable annotationHandler
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
