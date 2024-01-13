package com.aliucord.manager.ui.screens.plugins

import android.util.Log
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.aliucordDir
import com.aliucord.manager.domain.model.Plugin
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

class PluginsModel : ScreenModel {
    var showChangelogDialog by mutableStateOf(false)
        private set

    var showUninstallDialog by mutableStateOf(false)
        private set

    var search by mutableStateOf("")
        private set

    var selectedPlugin by mutableStateOf<Plugin?>(null)
        private set

    var error by mutableStateOf(false)
        private set

    val plugins = mutableStateListOf<Plugin>()
    val enabled = mutableStateMapOf<String, Boolean>()

    fun search(search: String) {
        this.search = search
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

    private val settingsFile = aliucordDir.resolve("settings/Aliucord.json")

    fun setPluginEnabled(plugin: String, enable: Boolean) {
        screenModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                enabled.clear()
                enabled.putAll(loadEnabled())
                enabled[plugin] = enable
            }

            @OptIn(ExperimentalSerializationApi::class)
            try {
                val settings = settingsFile.inputStream()
                    .use { Json.decodeFromStream<Map<String, JsonElement>>(it) }
                    .toMutableMap()

                settings["AC_PM_$plugin"] = JsonPrimitive(enable)

                settingsFile.outputStream()
                    .use { Json.encodeToStream(settings, it) }
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to write Aliucord.json: ${Log.getStackTraceString(t)}")
                withContext(Dispatchers.Main) { error = true }
            }
        }
    }

    private suspend fun loadEnabled(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        if (!settingsFile.exists()) return@withContext emptyMap()

        @OptIn(ExperimentalSerializationApi::class)
        try {
            settingsFile.inputStream()
                .use { Json.decodeFromStream<Map<String, JsonElement>>(it) }
                .filterKeys { it.startsWith("AC_PM_") }
                .mapKeys { it.key.substring("AC_PM_".length) }
                .mapValues { it.value.jsonPrimitive.boolean }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to load Aliucord.json ${Log.getStackTraceString(t)}")
            withContext(Dispatchers.Main) { error = true }
            emptyMap()
        }
    }

    init {
        screenModelScope.launch {
            enabled.putAll(loadEnabled())
        }

        screenModelScope.launch(Dispatchers.IO) {
            val pluginsDir = aliucordDir.resolve("plugins")

            if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
                Log.e(BuildConfig.TAG, "Failed to create plugins dir. Missing Permissions?")
                return@launch
            }

            val files = pluginsDir.listFiles { file -> file.extension == "zip" } ?: return@launch
            val loadedPlugins = files.mapNotNull { file ->
                try {
                    Plugin(file)
                } catch (e: Exception) {
                    Log.e(BuildConfig.TAG, "Failed to load plugin ${file.name}", e)
                    null
                }
            }.sortedBy { it.manifest.name }

            withContext(Dispatchers.Main) {
                plugins.addAll(loadedPlugins)
            }
        }
    }
}
