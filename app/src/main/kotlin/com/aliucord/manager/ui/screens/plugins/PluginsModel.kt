package com.aliucord.manager.ui.screens.plugins

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.ui.screens.plugins.model.PluginItem
import com.aliucord.manager.ui.screens.plugins.model.PluginManifest
import com.aliucord.manager.ui.util.emptyImmutableList
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.*
import com.github.diamondminer88.zip.ZipReader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.File
import kotlin.time.Duration

class PluginsModel(
    private val context: Application,
    private val paths: PathManager,
    private val json: Json,
) : ScreenModel {
    private val plugins = MutableStateFlow<ImmutableList<PluginItem>>(emptyImmutableList())
    private val aliucordJsonMutex = Mutex()

    var error by mutableStateOf(false)
        private set

    var showChangelogDialog by mutableStateOf<PluginItem?>(null)
        private set

    var showUninstallDialog by mutableStateOf<PluginItem?>(null)
        private set

    val searchText: StateFlow<String>
        private field = MutableStateFlow("")

    var pluginsSafeMode = MutableStateFlow(false)
        private set

    val filteredPlugins: StateFlow<ImmutableList<PluginItem>> = searchText
        .combine(plugins) { searchText, plugins ->
            if (searchText.isBlank()) {
                plugins
            } else {
                plugins.filter { plugin ->
                    plugin.manifest.name.contains(searchText, ignoreCase = true)
                        || plugin.manifest.description.contains(searchText, ignoreCase = true)
                        || plugin.manifest.authors.any { (name) -> name.contains(searchText, ignoreCase = true) }
                }.toUnsafeImmutable()
            }
        }.stateIn(
            scope = screenModelScope + Dispatchers.Default,
            started = SharingStarted.WhileSubscribed(replayExpiration = Duration.ZERO),
            initialValue = plugins.value,
        )

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadSafeMode() = screenModelScope.launchIO {
        val settings = paths.coreSettingsFile.inputStream()
            .use { Json.decodeFromStream<Map<String, JsonElement>>(it) }

        val safeMode = settings["AC_SAFE_MODE"]?.jsonPrimitive?.booleanOrNull ?: false
        pluginsSafeMode.value = safeMode
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun setSafeMode(safeMode: Boolean) = screenModelScope.launchIO {
        val settings = paths.coreSettingsFile.inputStream()
            .use { Json.decodeFromStream<MutableMap<String, JsonElement>>(it) }

        settings["AC_SAFE_MODE"] = JsonPrimitive(safeMode)

        paths.coreSettingsFile.outputStream().use { out ->
            Json.encodeToStream(settings, out)
        }

        pluginsSafeMode.value = safeMode
    }

    fun setSearchText(search: String) {
        searchText.value = search
    }

    fun showChangelogDialog(plugin: PluginItem) {
        showChangelogDialog = plugin
    }

    fun hideChangelogDialog() {
        showChangelogDialog = null
    }

    fun showUninstallDialog(plugin: PluginItem) {
        showUninstallDialog = plugin
    }

    fun hideUninstallDialog() {
        showUninstallDialog = null
    }

    fun uninstallPlugin(plugin: PluginItem) = screenModelScope.launchIO {
        if (!plugins.value.any { it.path == plugin.path }) {
            mainThread { hideUninstallDialog() }
            return@launchIO
        }

        val deleteSuccess = try {
            File(plugin.path).delete()
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to delete plugin", t)
            false
        }

        if (!deleteSuccess) {
            mainThread {
                hideUninstallDialog()
                context.showToast(R.string.plugins_error)
            }
            return@launchIO
        }

        plugins.update { (it - plugin).toUnsafeImmutable() }
        mainThread { hideUninstallDialog() }
    }

    fun setPluginEnabled(pluginName: String, enabled: Boolean) = screenModelScope.launchIO {
        aliucordJsonMutex.withLock {
            @OptIn(ExperimentalSerializationApi::class)
            try {
                val settings = paths.coreSettingsFile.inputStream()
                    .use { Json.decodeFromStream<Map<String, JsonElement>>(it) }
                    .toMutableMap()

                settings["AC_PM_$pluginName"] = JsonPrimitive(enabled)

                paths.coreSettingsFile.outputStream()
                    .use { json.encodeToStream(settings, it) }
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to write Aliucord.json", t)
                mainThread { error = true }
            }
        }

        mainThread {
            plugins.value.forEach {
                if (it.manifest.name == pluginName)
                    it.enabled = enabled
            }
        }
    }

    // Called by screen to load initial data
    fun refreshData() = screenModelScope.launchIO {
        loadPlugins()
        loadPluginsEnabled()
        loadSafeMode()
    }

    private suspend fun loadPluginsEnabled() {
        if (!paths.coreSettingsFile.exists()) return

        aliucordJsonMutex.withLock {
            @OptIn(ExperimentalSerializationApi::class)
            val pluginToggles = try {
                paths.coreSettingsFile.inputStream()
                    .use { Json.decodeFromStream<Map<String, JsonElement>>(it) }
                    .filterKeys { it.startsWith("AC_PM_") }
                    .mapKeys { it.key.substring("AC_PM_".length) }
                    .mapValues { it.value.jsonPrimitive.boolean }
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to load Aliucord.json", t)
                mainThread { error = true }
                emptyMap()
            }

            mainThread {
                plugins.value.forEach {
                    if (!pluginToggles.getOrDefault(it.manifest.name, true))
                        it.enabled = false
                }
            }
        }
    }

    private suspend fun loadPlugins() {
        try {
            if (!paths.pluginsDir.exists() && !paths.pluginsDir.mkdirs())
                throw IllegalStateException("Failed to create plugins directory")

            val pluginFiles = paths.pluginsDir.listFiles { file -> file.extension == "zip" }
                ?: throw IllegalStateException("Failed to read plugins directory")

            val pluginItems = pluginFiles.map {
                PluginItem(
                    manifest = loadPluginManifest(it),
                    path = it.absolutePath,
                )
            }

            plugins.value = pluginItems.toUnsafeImmutable()
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to load plugins", t)
            mainThread { error = true }
        }
    }

    private fun loadPluginManifest(pluginFile: File): PluginManifest {
        return ZipReader(pluginFile).use {
            val manifest = it.openEntry("manifest.json")
                ?: throw Exception("Plugin ${pluginFile.name} has no manifest")

            try {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream(manifest.read().inputStream())
            } catch (t: Throwable) {
                throw Exception("Failed to parse plugin manifest for ${pluginFile.name}", t)
            }
        }
    }
}
