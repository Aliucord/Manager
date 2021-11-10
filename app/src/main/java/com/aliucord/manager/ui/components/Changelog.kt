package com.aliucord.manager.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

private val hyperLinkPattern = Pattern.compile("\\[(.+?)]\\((.+?\\))")

@SuppressLint("ComposableNaming") // Can't use MaterialTheme without Composable, but this is not a component
@Composable
private fun AnnotatedString.Builder.hyperlink(content: String, outTags: ArrayList<String>) {
    var idx = 0
    with (hyperLinkPattern.matcher(content)) {
        while (find()) {
            val start = start()
            val end = end()
            val title = group(1)!!
            val url = group(2)!!

            append(content.substring(idx, start))
            withStyle(style = SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)) {
                append(title)
            }
            addStringAnnotation(title, url, start, end)
            idx = end + 1
            outTags.add(title)
        }
    }
    if (idx < content.length) append(content.substring(idx))
}

const val bulletPoint = '•'
@Composable
fun Changelog(changelog: String) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .fillMaxHeight(0.7f)
        .verticalScroll(scrollState)
    ) {
        changelog.lines().forEach {
            var line = it.trim()
            if (line.isNotEmpty()) {
                when (line[0]) {
                    '#' -> {
                        do {
                            line = line.substring(1);
                        } while (line.startsWith("#"))
                        Text(line.trimStart(), style = MaterialTheme.typography.h6)
                    }
                    '*' -> {
                        val tags = ArrayList<String>()
                        LinkText(
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                                    append(bulletPoint)
                                }
                                hyperlink(line.substring(1), tags)
                            }
                        )
                    }
                    else -> {
                        val tags = ArrayList<String>()
                        LinkText(
                            buildAnnotatedString {
                                hyperlink(line, tags)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkText(annotatedString: AnnotatedString) {
    val urlHandler = LocalUriHandler.current
    ClickableText(
        text = annotatedString,
        style = TextStyle(color = LocalContentColor.current),
        onClick = { offset ->
            annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let {
                urlHandler.openUri(it.item)
            }
        }
    )
}