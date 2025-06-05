package com.aliucord.manager.ui.screens.settings

import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.R
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    fun setKeepPatchedApks(value: Boolean) {
        // Disallow setting keep APKs if externalCacheDir doesn't exist (some ROMs)
        if (value && application.externalCacheDir == null) {
            screenModelScope.launch {
                delay(300)
                preferences.keepPatchedApks = false
                application.showToast(R.string.setting_keep_patched_apks_error)
            }
        }

        preferences.keepPatchedApks = value
    }

    fun clearCacheDir() = screenModelScope.launchBlock {
        application.externalCacheDir?.deleteRecursively()
        application.showToast(R.string.action_cleared_cache)
    }
}
