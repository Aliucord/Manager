package com.aliucord.manager.ui.screens.about

import com.aliucord.manager.network.models.Contributor
import kotlinx.collections.immutable.ImmutableList

sealed interface AboutScreenState {
    data object Loading : AboutScreenState
    data object Failure : AboutScreenState
    data class Loaded(val contributors: ImmutableList<Contributor>) : AboutScreenState
}
