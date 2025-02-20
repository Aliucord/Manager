package com.aliucord.manager.manager

import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import com.aliucord.manager.manager.base.BasePreferenceManager
import com.aliucord.manager.ui.components.Theme

@Stable
class PreferencesManager(preferences: SharedPreferences) : BasePreferenceManager(preferences) {
    var theme by enumPreference("theme", Theme.DARK)
    var dynamicColor by booleanPreference("dynamic_color", false)
    var devMode by booleanPreference("dev_mode", false)
    var installer by enumPreference<InstallerSetting>("installer", InstallerSetting.PM)
    var keepPatchedApks by booleanPreference("keep_patched_apks", false)
}
