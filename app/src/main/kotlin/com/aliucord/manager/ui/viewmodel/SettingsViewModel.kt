package com.aliucord.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.theme.Theme

class SettingsViewModel(
    private val application: Application,
    val preferences: PreferencesManager
) : ViewModel() {
    private val initialPackageName = preferences.packageName

    var showThemeDialog by mutableStateOf(false)
        private set

    var packageNameError by mutableStateOf(false)
        private set

    fun showThemeDialog() {
        showThemeDialog = true
    }

    fun hideThemeDialog() {
        showThemeDialog = false
    }

    fun setPackageName(packageName: String) {
        preferences.packageName = packageName
        packageNameError = !PACKAGE_REGEX.matches(packageName)
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

    override fun onCleared() {
        super.onCleared()

        if (!PACKAGE_REGEX.matches(preferences.packageName)) {
            preferences.packageName = initialPackageName
        }
    }

    companion object {
        private val PACKAGE_REGEX = "^[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*(?:\\.[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*)+$"
            .toRegex(RegexOption.IGNORE_CASE)
    }
}
