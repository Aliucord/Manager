/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component.plugins

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aliucord.manager.R
import com.aliucord.manager.model.Plugin

private val hyperLinkPattern = Regex("\\[(.+?)]\\((.+?\\))")

@Suppress("RegExpRedundantEscape") // It is very much not redundant and causes a crash lol
private val headerStylePattern = Regex("\\{(improved|added|fixed)( marginTop)?\\}")

@SuppressLint("ComposableNaming") // Can't use MaterialTheme without Composable, but this is not a component
@Composable
private fun AnnotatedString.Builder.hyperlink(content: String) {
    var idx = 0

    with(hyperLinkPattern.toPattern().matcher(content)) {
        while (find()) {
            val start = start()
            val end = end()
            val title = group(1)!!
            val url = group(2)!!

            append(content.substring(idx, start))

            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushStringAnnotation(title, url)
                append(title)
                pop()
            }

            idx = end
        }
    }

    if (idx < content.length) append(content.substring(idx))
}

@Composable
fun Changelog(
    plugin: Plugin,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_history_white_24dp),
                contentDescription = stringResource(R.string.view_plugin_changelog, plugin.manifest.name),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        title = { Text(plugin.manifest.name) },
        text = {
            Column {
                plugin.manifest.changelogMedia?.let { mediaUrl ->
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 90.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaUrl)
                            .build(),
                        placeholder = rememberVectorPainter(image = Icons.Default.Person),
                        contentDescription = stringResource(R.string.changelog_media)
                    )
                }

                LazyColumn(
                    modifier = Modifier.wrapContentHeight()
                ) {
                    items(plugin.manifest.changelog!!.lines()) {
                        var line = it.trim()

                        if (line.isNotEmpty()) {
                            when (line[0]) {
                                '#' -> {
                                    do {
                                        line = line.substring(1)
                                    } while (line.startsWith("#"))

                                    Text(
                                        line.trimStart(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                    )
                                }
                                '*' -> {
                                    LinkText(
                                        buildAnnotatedString {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("● ")
                                            }

                                            hyperlink(line.substring(1))
                                        },
                                        Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                else -> {
                                    when {
                                        line.endsWith("marginTop}") -> {
                                            val color = MaterialTheme.colorScheme.onSurface
                                            Text(
                                                line,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                            )
                                        }
                                        line.all { c -> c == '=' } -> {
                                        } // Discord ignores =======
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
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun LinkText(annotatedString: AnnotatedString, modifier: Modifier = Modifier) {
    val urlHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedString,
        style = TextStyle(color = LocalContentColor.current),
        onClick = { offset ->
            annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let {
                urlHandler.openUri(it.item)
            }
        },
        modifier = modifier
    )
}
