/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.component.plugins

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.domain.model.Plugin
import com.aliucord.manager.ui.util.joinToAnnotatedString

@Composable
fun PluginCard(
    plugin: Plugin,
    onClickDelete: () -> Unit,
    onClickShowChangelog: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isEnabled by rememberSaveable { mutableStateOf(true) }

    ElevatedCard {
        // Header
        Row(
            modifier = Modifier.clickable { isEnabled = !isEnabled }.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        plugin.manifest.authors.joinToAnnotatedString(
                            prefix = {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                                    append("By ")
                                }
                            }
                        ) { author ->
                            val start = this@joinToAnnotatedString.length
                            withStyle(SpanStyle(
                                textDecoration = TextDecoration.Underline
                            )) {
                                append(author.name)
                            }
                            addStringAnnotation("authorId", author.id.toString(), start, start + author.name.length)
                        }
                    }
                }

                ClickableText(
                    text = authors,
                    style = MaterialTheme.typography.labelMedium,
                    onClick = { offset ->
                        authors.getStringAnnotations("authorId", offset, offset)
                            .firstOrNull()
                            ?.let { (id) -> uriHandler.openUri("https://discord.com/users/$id") }
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

        Divider(modifier = Modifier.alpha(0.3f).padding(horizontal = 16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Description
            Text(
                modifier = Modifier
                    .heightIn(max = 150.dp, min = 40.dp)
                    .padding(bottom = 20.dp),
                text = plugin.manifest.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(25.dp),
                    onClick = {
                        uriHandler.openUri(plugin.manifest.repositoryUrl)
                    }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.ic_account_github_white_24dp),
                        contentDescription = stringResource(R.string.github)
                    )
                }

                if (plugin.manifest.changelog != null) {
                    IconButton(modifier = Modifier.size(25.dp), onClick = onClickShowChangelog) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.view_plugin_changelog, plugin.manifest.name)
                        )
                    }
                }

                Spacer(Modifier.weight(1f, true))

                IconButton(modifier = Modifier.size(25.dp), onClick = onClickDelete) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.uninstall),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
