package com.aliucord.manager.ui.screens.install

import com.aliucord.manager.ui.screens.install.InstallScreenState.*

sealed interface InstallScreenState {
    data object Pending : InstallScreenState
    data object Working : InstallScreenState
    data object Success : InstallScreenState

    data class Failed(
        val failureLog: String,
    ) : InstallScreenState

    data object CloseScreen : InstallScreenState
}

val InstallScreenState.isNewlyFinished: Boolean
    inline get() = this == Success || this is Failed

val InstallScreenState.isFinished: Boolean
    inline get() = isNewlyFinished || this == CloseScreen
