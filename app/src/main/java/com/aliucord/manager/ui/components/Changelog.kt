package com.aliucord.manager.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.utils.Plugin
import java.util.regex.Pattern

private val hyperLinkPattern = Pattern.compile("\\[(.+?)]\\((.+?\\))")

@SuppressLint("ComposableNaming") // Can't use MaterialTheme without Composable, but this is not a component
@Composable
private fun AnnotatedString.Builder.hyperlink(content: String) {
    var idx = 0
    with (hyperLinkPattern.matcher(content)) {
        while (find()) {
            val start = start()
            val end = end()
            val title = group(1)!!
            val url = group(2)!!

            append(content.substring(idx, start))
            withStyle(style = SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)) {
                pushStringAnnotation(title, url)
                append(title)
                pop()
            }
            idx = end
        }
    }
    if (idx < content.length) append(content.substring(idx))
}

const val bulletPoint = '•'
@Composable
fun Changelog(plugin: Plugin, showDialog: MutableState<Boolean>) {
    Column(modifier = Modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxHeight(0.9f)
        .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.changelog, plugin.manifest.name),
            style = MaterialTheme.typography.h4
        )
        Divider(color = MaterialTheme.colors.primary)
        LazyColumn(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .fillMaxHeight(0.92f)
        ) {
            items(plugin.manifest.changelog!!.lines()) {
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
                            LinkText(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)) {
                                        append(bulletPoint)
                                    }
                                    hyperlink(line.substring(1))
                                }
                            )
                        }
                        else -> {
                            LinkText(
                                buildAnnotatedString {
                                    hyperlink(line)
                                }
                            )
                        }
                    }
                }
            }
        }
        Button(onClick = { showDialog.value = false }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.close))
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