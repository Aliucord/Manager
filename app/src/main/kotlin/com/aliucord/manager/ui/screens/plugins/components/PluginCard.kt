/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.plugins.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.aliucord.manager.ui.screens.plugins.model.PluginItem

@Composable
fun PluginCard(
    plugin: PluginItem,
    onClickDelete: () -> Unit,
    onClickShowChangelog: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    ElevatedCard {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onSetEnabled(!plugin.enabled) }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        ) {
            Column {
                // Name
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(plugin.manifest.name)
                        }
                        append(" v")
                        append(plugin.manifest.version)
                    }
                )

                // Authors
                val authors = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        for ((idx, author) in plugin.manifest.authors.withIndex()) {
                            if (idx > 0) append(", ")

                            if (author.hyperlink) pushLink(
                                LinkAnnotation.Url(
                                    url = author.discordUrl,
                                    styles = TextLinkStyles(
                                        SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                        )
                                    ),
                                )
                            )
                            append(author.name)
                            if (author.hyperlink) pop()
                        }
                    }
                }
                Text(
                    text = authors,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.weight(1f, true))

            // Toggle Switch
            Switch(
                checked = plugin.enabled,
                onCheckedChange = { onSetEnabled(!plugin.enabled) }
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .alpha(0.3f)
                .padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Description
            Text(
                text = plugin.manifest.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .heightIn(max = 150.dp, min = 40.dp)
                    .padding(bottom = 20.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { uriHandler.openUri(plugin.manifest.repositoryUrl) },
                    modifier = Modifier.size(25.dp),
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.ic_account_github_white_24dp),
                        contentDescription = stringResource(R.string.github)
                    )
                }

                if (plugin.manifest.changelog != null) {
                    IconButton(
                        onClick = onClickShowChangelog,
                        modifier = Modifier.size(25.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.plugins_view_changelog),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.weight(1f, true))

                IconButton(
                    onClick = onClickDelete,
                    modifier = Modifier.size(25.dp),
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.ic_delete_forever),
                        contentDescription = stringResource(R.string.action_uninstall),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
