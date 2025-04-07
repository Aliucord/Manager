package com.aliucord.manager.ui.screens.patching

import com.aliucord.manager.ui.screens.patching.PatchingScreenState.CloseScreen

sealed interface PatchingScreenState {
    data object Working : PatchingScreenState
    data object Success : PatchingScreenState
    data class Failed(val installId: String) : PatchingScreenState
    data object CloseScreen : PatchingScreenState
}

val PatchingScreenState.isProgressChange: Boolean
    inline get() = this != CloseScreen

val PatchingScreenState.isFinished: Boolean
    inline get() = isProgressChange || this == CloseScreen
