package com.aliucord.manager.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.aliucordDir
import com.aliucord.manager.domain.model.Plugin
import kotlinx.coroutines.*

class PluginsViewModel : ViewModel() {
    var showChangelogDialog by mutableStateOf(false)
        private set

    var showUninstallDialog by mutableStateOf(false)
        private set

    var search by mutableStateOf("")
        private set

    var selectedPlugin by mutableStateOf<Plugin?>(null)
        private set

    val plugins = mutableStateListOf<Plugin>()

    init {
        viewModelScope.launch {
            loadPlugins()
        }
    }

    fun search(search: String) {
        this.search = search
    }

    fun clearSearch() {
        search = ""
    }

    fun showChangelogDialog(plugin: Plugin) {
        showChangelogDialog = true
        selectedPlugin = plugin
    }

    fun hideChangelogDialog() {
        showChangelogDialog = false
    }

    fun showUninstallDialog(plugin: Plugin) {
        showUninstallDialog = true
        selectedPlugin = plugin
    }

    fun hideUninstallDialog() {
        showUninstallDialog = false
    }

    fun uninstallPlugin(plugin: Plugin) {
        plugin.file.delete()
        plugins.remove(plugin)
    }

    private suspend fun loadPlugins(): Unit = withContext(Dispatchers.IO) {
        val pluginsDir = aliucordDir.resolve("plugins")

        if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
            Log.e(BuildConfig.TAG, "Failed to create plugins dir. Missing Permissions?")
            return@withContext
        }

        val files = pluginsDir.listFiles { file -> file.extension == "zip" } ?: return@withContext

        plugins.addAll(
            files.mapNotNull { file ->
                try {
                    Plugin(file)
                } catch (e: Exception) {
                    Log.e(BuildConfig.TAG, "Failed to load plugin ${file.nameWithoutExtension}", e)
                    null
                }
            }.sortedBy { it.manifest.name }
        )
    }
}
