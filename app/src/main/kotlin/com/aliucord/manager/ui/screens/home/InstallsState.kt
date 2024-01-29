package com.aliucord.manager.ui.screens.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface InstallsState {
    data object None : InstallsState
    data object Error : InstallsState
    data object Fetching : InstallsState
    data class Fetched(val data: ImmutableList<InstallData>) : InstallsState
}
