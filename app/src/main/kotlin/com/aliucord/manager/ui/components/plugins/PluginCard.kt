package com.aliucord.manager.ui.components.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.Switch
import com.aliucord.manager.utils.Plugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginCard(
    plugin: Plugin,
    onDelete: () -> Unit,
    onShowChangelog: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
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

                    // Author(s)
                    Text(
                        "By ${plugin.manifest.authors.joinToString { it.name }}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.weight(1f, true))

                // Toggle Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }

            // Description
            Text(
                text = plugin.manifest.description,
                modifier = Modifier.heightIn(min = 50.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            // Buttons
            Row(Modifier.fillMaxWidth()) {
                val uriHandler = LocalUriHandler.current

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
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onSurface)
                }

                if (plugin.manifest.changelog != null) {
                    IconButton(
                        onClick = onShowChangelog
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history_white_24dp),
                            contentDescription = stringResource(R.string.view_plugin_changelog, plugin.manifest.name),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.weight(1f, true))

                IconButton(onClick = onDelete) {
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