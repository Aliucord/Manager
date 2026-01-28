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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import kotlin.time.Duration

class PluginsModel(
    private val context: Application,
    private val paths: PathManager,
    private val json: Json,
) : ScreenModel {
    private val plugins = MutableStateFlow<ImmutableList<PluginItem>>(emptyImmutableList())

    var error by mutableStateOf(false)
        private set

    var showChangelogDialog by mutableStateOf<PluginItem?>(null)
        private set

    var showUninstallDialog by mutableStateOf<PluginItem?>(null)
        private set

    val searchText: StateFlow<String>
        field = MutableStateFlow("")

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

    // ---- State setters ---- //

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

    // ---- IO state setters ---- //

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
        try {
            editAliucordSettings {
                put(JsonPrimitive("AC_PM_$pluginName"), JsonPrimitive(enabled))
            }
            mainThread {
                plugins.value.forEach {
                    if (it.manifest.name == pluginName)
                        it.enabled = enabled
                }
            }
        } catch (e: Exception) {
            Log.e(BuildConfig.TAG, "Failed to toggle plugin", e)
            mainThread { context.showToast(R.string.status_failed) }
        }
    }

    fun setSafeMode(safeMode: Boolean) = screenModelScope.launchIO {
        try {
            editAliucordSettings {
                put(JsonPrimitive("AC_aliucord_safe_mode_enabled"), JsonPrimitive(safeMode))
            }
            pluginsSafeMode.value = safeMode
        } catch (e: Exception) {
            Log.e(BuildConfig.TAG, "Failed to toggle plugin", e)
            mainThread { context.showToast(R.string.status_failed) }
        }
    }

    // ---- State loading ---- //

    // Called by screen to load initial data
    fun refreshData() = screenModelScope.launchIO {
        try {
            loadSafeMode()
            loadPlugins()
            loadPluginsEnabled()
        } catch (e: Exception) {
            Log.e(BuildConfig.TAG, "Failed to load plugins state", e)
            mainThread {
                context.showToast(R.string.plugins_error)
                error = true
            }
        }
    }

    private fun loadSafeMode() = screenModelScope.launchIO {
        @Serializable
        data class SafeModeSettings(
            @SerialName("AC_aliucord_safe_mode_enabled")
            val safeMode: Boolean,
        )

        pluginsSafeMode.value = readAliucordSettings<SafeModeSettings>()?.safeMode ?: false
    }

    private suspend fun loadPluginsEnabled() {
        val pluginToggles = readAliucordSettings<Map<JsonPrimitive, JsonElement>>()
            ?.filterKeys { it.isString && it.content.startsWith("AC_PM_") }
            ?.filterValues { (it as? JsonPrimitive)?.booleanOrNull == true }
            ?.mapKeys { (key, _) -> key.content.substring("AC_PM_".length) }
            ?.mapValues { (_, value) -> value.jsonPrimitive.boolean }

        if (pluginToggles != null) mainThread {
            plugins.value.forEach {
                if (!pluginToggles.getOrDefault(it.manifest.name, true))
                    it.enabled = false
            }
        }
    }

    private fun loadPlugins() {
        if (!paths.pluginsDir.exists() && !paths.pluginsDir.mkdirs())
            throw IllegalStateException("Failed to create plugins directory")

        val pluginFiles = paths.pluginsDir.listFiles { file -> file.extension == "zip" }
            ?: throw IllegalStateException("Failed to read plugins directory")

        val pluginItems = pluginFiles
            .map {
                PluginItem(
                    manifest = loadPluginManifest(it),
                    path = it.absolutePath,
                )
            }
            .sortedBy { it.manifest.name }

        plugins.value = pluginItems.toUnsafeImmutable()
    }

    private fun loadPluginManifest(pluginFile: File): PluginManifest {
        return ZipReader(pluginFile).use {
            val manifest = it.openEntry("manifest.json")
                ?: throw Exception("Plugin ${pluginFile.name} has no manifest")

            try {
                json.decodeFromStream(manifest.read().inputStream())
            } catch (t: Throwable) {
                throw Exception("Failed to parse plugin manifest for ${pluginFile.name}", t)
            }
        }
    }

    // ---- Aliucord settings ---- //

    /**
     * Reads Aliucord core's settings, applies [block] to it, and writes it back.
     */
    private suspend fun editAliucordSettings(block: (MutableMap<JsonPrimitive, JsonElement>).() -> Unit) {
        SETTINGS_MUTEX.withLock {
            val settings = try {
                if (paths.coreSettingsFile.exists()) {
                    json.decodeFromStream<MutableMap<JsonPrimitive, JsonElement>>(paths.coreSettingsFile.inputStream())
                } else {
                    mutableMapOf()
                }
            } catch (e: Exception) {
                Log.e(BuildConfig.TAG, "Aliucord settings are corrupted!", e)
                mutableMapOf()
            }

            // Apply modifier block
            block(settings)

            paths.coreSettingsFile.parentFile!!.mkdirs()
            paths.coreSettingsFile.outputStream()
                .use { out -> json.encodeToStream(settings, out) }
        }
    }

    /**
     * Reads Aliucord core's settings and parses it into a specific model.
     * This should not be used for future writes.
     *
     * @return The parsed settings model, or null if settings are missing or corrupt.
     */
    private suspend inline fun <reified T> readAliucordSettings(): T? {
        return SETTINGS_MUTEX.withLock {
            try {
                if (paths.coreSettingsFile.exists()) {
                    json.decodeFromStream<T>(paths.coreSettingsFile.inputStream())
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(BuildConfig.TAG, "Aliucord settings are corrupted!", e)
                null
            }
        }
    }

    private companion object {
        /**
         * Global lock on the main Aliucord settings.
         */
        private val SETTINGS_MUTEX = Mutex()
    }
}
