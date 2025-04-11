package com.aliucord.manager.ui.previews.screens.plugins

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.plugins.PluginsScreenContent
import com.aliucord.manager.ui.screens.plugins.model.PluginItem
import com.aliucord.manager.ui.screens.plugins.model.PluginManifest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

// This preview has scrollable/interactable content that cannot be tested from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun PluginsScreenLoadedPreview() {
    val filterState = remember { mutableStateOf("test") }

    ManagerTheme {
        PluginsScreenContent(
            searchText = filterState,
            setSearchText = filterState::value::set,
            isError = false,
            plugins = plugins,
            onPluginUninstall = {},
            onPluginChangelog = {},
            onPluginToggle = { name, enabled -> },
        )
    }
}

private val plugins: ImmutableList<PluginItem> = persistentListOf(
    PluginItem(
        path = UUID.randomUUID().toString(),
        manifest = PluginManifest(
            name = "CloseDMs",
            authors = persistentListOf(
                PluginManifest.Author(name = "Diamond", id = 0L),
            ),
            description = "Shortcut to close DMs in the DM context menu.",
            version = "1.0.0",
            updateUrl = "",
            changelog = "",
            changelogMedia = null,
        )
    ),
    PluginItem(
        path = UUID.randomUUID().toString(),
        manifest = PluginManifest(
            name = "ConfigurableStickerSizes",
            authors = persistentListOf(
                PluginManifest.Author(name = "rushii", id = 0L, hyperlink = false),
            ),
            description = "Makes sticker sizes configurable.",
            version = "1.1.5",
            updateUrl = "",
            changelog = "",
            changelogMedia = null,
        )
    ),
    PluginItem(
        path = UUID.randomUUID().toString(),
        manifest = PluginManifest(
            name = "AudioPlayer",
            authors = persistentListOf(
                PluginManifest.Author(name = "rushii", id = 0L, hyperlink = false),
            ),
            description = "Makes audio files playable.",
            version = "0.0.1",
            updateUrl = "",
            changelog = "",
            changelogMedia = null,
        )
    ),
    PluginItem(
        path = UUID.randomUUID().toString(),
        manifest = PluginManifest(
            name = "TypingIndicators",
            authors = persistentListOf(
                PluginManifest.Author(name = "rushii", id = 0L, hyperlink = false),
            ),
            description = "Adds typing indicators to channels that people are currently typing in.",
            version = "1.1.0",
            updateUrl = "",
            changelog = "",
            changelogMedia = null,
        )
    ),
)
