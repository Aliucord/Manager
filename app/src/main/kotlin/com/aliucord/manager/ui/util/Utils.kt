package com.aliucord.manager.ui.util

import androidx.compose.ui.text.AnnotatedString

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
