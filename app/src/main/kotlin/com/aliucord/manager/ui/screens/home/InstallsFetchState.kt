package com.aliucord.manager.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.aliucord.manager.ui.util.DiscordVersion
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface InstallsFetchState {
    data object None : InstallsFetchState
    data object Error : InstallsFetchState
    data object Fetching : InstallsFetchState
    data class Fetched(val data: ImmutableList<Data>) : InstallsFetchState

    @Immutable
    data class Data(
        val name: String,
        val packageName: String,
        val version: DiscordVersion,
        val icon: BitmapPainter,
        val baseUpdated: Boolean,
    )
}
