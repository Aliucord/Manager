package com.aliucord.manager.ui.screens.plugins.model

import androidx.compose.runtime.*

@Stable
data class PluginItem(
    val manifest: PluginManifest,
    val path: String,
) {
    // Plugins are enabled by default unless disabled in Aliucord settings
    var enabled by mutableStateOf(true)
}
