/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens.plugins

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.screens.plugins.components.*
import com.aliucord.manager.ui.screens.plugins.components.dialogs.UninstallPluginDialog
import com.aliucord.manager.ui.screens.plugins.model.PluginItem
import com.aliucord.manager.ui.util.paddings.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class PluginsScreen : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "Plugins"

    @Composable
    override fun Content() {
        val model = koinScreenModel<PluginsModel>()

        // Refresh plugins list on activity resume or when this initially opens
        LifecycleResumeEffect(Unit) {
            model.refreshData()

            onPauseOrDispose {}
        }

        model.showUninstallDialog?.let { plugin ->
            UninstallPluginDialog(
                pluginName = plugin.manifest.name,
                onConfirm = { model.uninstallPlugin(plugin) },
                onDismiss = model::hideUninstallDialog
            )
        }

        model.showChangelogDialog?.let { plugin ->
            Changelog(
                plugin = plugin,
                onDismiss = model::hideChangelogDialog
            )
        }

        PluginsScreenContent(
            searchText = model.searchText.collectAsState(),
            setSearchText = model::setSearchText,
            isError = model.error,
            plugins = model.filteredPlugins.collectAsState().value,
            onPluginUninstall = model::showUninstallDialog,
            onPluginChangelog = model::showChangelogDialog,
            onPluginToggle = model::setPluginEnabled,
        )
    }
}

@Composable
fun PluginsScreenContent(
    searchText: State<String>,
    setSearchText: (String) -> Unit,
    isError: Boolean,
    plugins: ImmutableList<PluginItem>,
    onPluginUninstall: (PluginItem) -> Unit,
    onPluginChangelog: (PluginItem) -> Unit,
    onPluginToggle: (name: String, enabled: Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plugins_title)) },
                navigationIcon = { BackButton() },
            )
        }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.exclude(PaddingValuesSides.Bottom))
                .padding(horizontal = 20.dp)
        ) {
            PluginSearch(
                currentFilter = searchText,
                onFilterChange = setSearchText,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = paddingValues
                    .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top)
                    .add(PaddingValues(vertical = 12.dp)),
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isError -> item(key = "ERROR") {
                        PluginsError(modifier = Modifier.fillParentMaxSize())
                    }

                    plugins.isNotEmpty() -> {
                        items(
                            items = plugins,
                            contentType = { "PLUGIN" },
                            key = { it.path },
                        ) { plugin ->
                            PluginCard(
                                plugin = plugin,
                                onClickDelete = { onPluginUninstall(plugin) },
                                onClickShowChangelog = { onPluginChangelog(plugin) },
                                onSetEnabled = { onPluginToggle(plugin.manifest.name, it) },
                            )
                        }
                    }

                    else -> item(key = "PLUGINS_NONE") {
                        PluginsNone(modifier = Modifier.fillParentMaxSize())
                    }
                }
            }
        }
    }
}
