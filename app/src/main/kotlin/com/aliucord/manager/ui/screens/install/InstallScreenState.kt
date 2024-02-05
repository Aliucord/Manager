package com.aliucord.manager.ui.screens.install

sealed interface InstallScreenState {
    data object Pending : InstallScreenState
    data object Working : InstallScreenState
    data object Success : InstallScreenState

    data class Failed(
        val failureLog: String,
    ) : InstallScreenState

    data object CloseScreen : InstallScreenState

    val isFinished: Boolean
        get() = this !is Pending && this !is Working
}
