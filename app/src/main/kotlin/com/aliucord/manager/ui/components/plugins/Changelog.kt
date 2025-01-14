/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.domain.model.Plugin

private val hyperLinkPattern = Regex("\\[(.+?)]\\((.+?\\))")

@Suppress("RegExpRedundantEscape") // It is very much not redundant and causes a crash lol
private val headerStylePattern = Regex("\\{(improved|added|fixed)( marginTop)?\\}")

@Composable
private fun AnnotatedString.Builder.MarkdownHyperlink(content: String) {
    var idx = 0

    with(hyperLinkPattern.toPattern().matcher(content)) {
        while (find()) {
            val start = start()
            val end = end()
            val title = group(1)!!
            val url = group(2)!!

            append(content.substring(idx, start))

            // @formatter:off
            pushLink(LinkAnnotation.Url(
                url,
                TextLinkStyles(SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ))
            ))
            append(title)
            pop()
            // @formatter:on

            idx = end
        }
    }

    if (idx < content.length) append(content.substring(idx))
}

@Composable
fun Changelog(
    plugin: Plugin,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.plugins_view_changelog, plugin.manifest.name)
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
                        model = mediaUrl,
                        contentDescription = stringResource(R.string.plugins_changelog_media)
                    )
                }

                LazyColumn {
                    items(plugin.manifest.changelog!!.lines()) {
                        var line = it.trim()

                        if (line.isNotEmpty()) {
                            when (line[0]) {
                                '#' -> {
                                    do {
                                        line = line.substring(1)
                                    } while (line.startsWith("#"))

                                    Text(
                                        text = line.trimStart(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                    )
                                }

                                '*' -> {
                                    Text(
                                        modifier = Modifier.padding(bottom = 2.dp),
                                        text = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("● ")
                                            }

                                            MarkdownHyperlink(line.substring(1))
                                        }
                                    )
                                }

                                else -> {
                                    when {
                                        line.endsWith("marginTop}") -> {
                                            val color = MaterialTheme.colorScheme.onSurface

                                            Text(
                                                text = line,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                            )
                                        }

                                        line.all { c -> c == '=' } -> {} // Discord ignores =======
                                        else -> {
                                            Text(buildAnnotatedString {
                                                MarkdownHyperlink(line)
                                            })
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
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
