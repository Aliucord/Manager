/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.model.Plugin

@Composable
fun PluginCard(
    plugin: Plugin,
    onDelete: () -> Unit,
    onShowChangelog: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            var isEnabled by remember { mutableStateOf(true) }

            // Header
            Row(
                modifier = Modifier.toggleable(value = isEnabled) {
                    isEnabled = it
                }
            ) {
                Column {
                    // Name
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(plugin.manifest.name)
                            }

                            append(" v${plugin.manifest.version}")
                        }
                    )

                    // Authors
                    val authors = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                            append("By ")
                        }

                        val iterator = plugin.manifest.authors.iterator()

                        iterator.forEach { author ->
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                val start = this@buildAnnotatedString.length
                                append(author.name)
                                addStringAnnotation("authorId", author.id.toString(), start, start + author.name.length)
                            }

                            if (iterator.hasNext()) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                                    append(", ")
                                }
                            }
                        }
                    }
                    ClickableText(
                        text = authors,
                        style = MaterialTheme.typography.labelMedium,
                        onClick = {
                            authors.getStringAnnotations("authorId", it, it)
                                .firstOrNull()
                                ?.let { (id) ->
                                    val url = "https://discord.com/users/$id"
                                    uriHandler.openUri(url)
                                }
                        }
                    )
                }

                Spacer(Modifier.weight(1f, true))

                // Toggle Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }

            Divider(modifier = Modifier.alpha(0.1f))

            // Description
            Text(
                text = plugin.manifest.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .heightIn(max = 150.dp, min = 40.dp)
                    .padding(top = 10.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = {
                        uriHandler.openUri(
                            plugin.manifest.updateUrl
                                .replace("raw.githubusercontent.com", "github.com")
                                .replaceFirst("/builds.*".toRegex(), "")
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_account_github_white_24dp),
                        contentDescription = "GitHub"
                    )
                }

                if (plugin.manifest.changelog != null) {
                    IconButton(onClick = onShowChangelog) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history_white_24dp),
                            contentDescription = stringResource(R.string.view_plugin_changelog, plugin.manifest.name)
                        )
                    }
                }

                Spacer(Modifier.weight(1f, true))

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.uninstall_plugin, plugin.manifest.name),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
