package com.aliucord.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.R
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.util.showToast
import com.aliucord.manager.util.throttle

class SettingsViewModel(
    private val application: Application,
    val preferences: PreferencesManager
) : ViewModel() {
    private val checkPackageNameThrottled = throttle(50L, viewModelScope) {
        packageNameError = !PACKAGE_REGEX.matches(preferences.packageName)
    }

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
        checkPackageNameThrottled()
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
        application.externalCacheDir?.deleteRecursively()
        application.showToast(R.string.action_cleared_cache)
    }

    init {
        checkPackageNameThrottled()
    }

    override fun onCleared() {
        if (!PACKAGE_REGEX.matches(preferences.packageName)) {
            preferences.packageName = "com.aliucord"
            application.showToast(R.string.settings_invalid_package_name)
        }
    }

    companion object {
        private val PACKAGE_REGEX = "^[a-z]\\w*(\\.[a-z]\\w*)+\$"
            .toRegex(RegexOption.IGNORE_CASE)
    }
}
