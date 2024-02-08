package com.aliucord.manager.ui.screens.settings

import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.Theme
import com.aliucord.manager.util.showToast

class SettingsModel(
    private val application: Application,
    val preferences: PreferencesManager,
) : ScreenModel {
    var showThemeDialog by mutableStateOf(false)
        private set

    fun showThemeDialog() {
        showThemeDialog = true
    }

    fun hideThemeDialog() {
        showThemeDialog = false
    }

    fun setTheme(theme: Theme) {
        preferences.theme = theme
    }

    fun clearCacheDir() {
        application.externalCacheDir?.deleteRecursively()
        application.showToast(R.string.action_cleared_cache)
    }
}
