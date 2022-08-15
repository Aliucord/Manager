package com.aliucord.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.theme.Theme

class SettingsViewModel(
    private val application: Application,
    val preferences: PreferencesManager
) : ViewModel() {
    var showThemeDialog by mutableStateOf(false)
        private set

    fun showThemeDialog() {
        showThemeDialog = true
    }

    fun hideThemeDialog() {
        showThemeDialog = false
    }

    fun setPackageName(packageName: String) {
        if (packageName.isNotBlank()) preferences.packageName = packageName
    }

    fun setAppName(appName: String) {
        if (appName.isNotBlank()) preferences.appName = appName
    }

    fun setTheme(theme: Theme) {
        preferences.theme = theme
    }

    fun setVersion(version: String) {
        if (version.isNotBlank()) preferences.version = version
    }

    fun clearCacheDir() {
        application.cacheDir.deleteRecursively()
    }
}
