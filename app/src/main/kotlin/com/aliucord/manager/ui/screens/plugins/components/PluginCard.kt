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
import com.aliucord.manager.ui.util.annotatingStringResource
import kotlinx.collections.immutable.persistentListOf

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
            modifier = Modifier
                .clickable { onSetEnabled(!plugin.enabled) }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Name
                Text(
                    annotatingStringResource(
                        R.string.plugins_plugin_title,
                        persistentListOf(
                            plugin.manifest.name,
                            plugin.manifest.version,
                        )
                    ) {
                        when (it) {
                            "plugin" -> SpanStyle(fontWeight = FontWeight.Bold)
                            else -> null
                        }
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
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.plugins_view_changelog, plugin.manifest.name),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.weight(1f, true))

                IconButton(modifier = Modifier.size(25.dp), onClick = onClickDelete) {
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
