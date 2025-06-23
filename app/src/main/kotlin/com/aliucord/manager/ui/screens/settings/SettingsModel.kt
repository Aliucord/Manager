package com.aliucord.manager.ui.screens.settings

import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.theme.Theme
import com.aliucord.manager.util.*

class SettingsModel(
    private val application: Application,
    private val paths: PathManager,
    val preferences: PreferencesManager,
) : ScreenModel {
    val installInfo = InstallInfo

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
        preferences.keepPatchedApks = value
    }

    fun clearCache() = screenModelScope.launchBlock {
        paths.clearCache()
        application.showToast(R.string.action_cleared_cache)
    }

    fun copyInstallInfo() {
        application.copyToClipboard(installInfo)
        application.showToast(R.string.action_copied)
    }

    companion object {
        @Suppress("KotlinConstantConditions")
        private val InstallInfo: String = """
            Aliucord Manager
            Version: ${BuildConfig.VERSION_NAME}
            Version Code: ${BuildConfig.VERSION_CODE}
            Release: ${if (BuildConfig.RELEASE) "Yes" else "No"}
            Git Branch: ${BuildConfig.GIT_BRANCH}
            Git Commit: ${BuildConfig.GIT_COMMIT}
            Git Changes: ${if (BuildConfig.GIT_LOCAL_CHANGES) "Yes" else "No"}
        """.trimIndent()
    }
}
