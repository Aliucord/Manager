package com.aliucord.manager.ui.screens.installopts

import androidx.compose.runtime.Immutable

@Immutable
data class InstallOptions(
    val appName: String,
    val packageName: String,
    val debuggable: Boolean,
    val replaceIcon: Boolean,
)
