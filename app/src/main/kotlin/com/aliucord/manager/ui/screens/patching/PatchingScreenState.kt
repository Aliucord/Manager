package com.aliucord.manager.ui.screens.patching

import com.aliucord.manager.ui.screens.patching.PatchingScreenState.*

sealed interface PatchingScreenState {
    data object Pending : PatchingScreenState
    data object Working : PatchingScreenState
    data object Success : PatchingScreenState

    data class Failed(
        val failureLog: String,
    ) : PatchingScreenState

    data object CloseScreen : PatchingScreenState
}

val PatchingScreenState.isNewlyFinished: Boolean
    inline get() = this == Success || this is Failed

val PatchingScreenState.isFinished: Boolean
    inline get() = isNewlyFinished || this == CloseScreen
