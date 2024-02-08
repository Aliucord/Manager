package com.aliucord.manager.manager

import android.content.SharedPreferences
import com.aliucord.manager.manager.base.BasePreferenceManager
import com.aliucord.manager.ui.components.Theme

class PreferencesManager(preferences: SharedPreferences) : BasePreferenceManager(preferences) {
    var theme by enumPreference("theme", Theme.SYSTEM)
    var dynamicColor by booleanPreference("dynamic_color", true)
    var devMode by booleanPreference("dev_mode", false)
    var installer by enumPreference("installer", InstallerSetting.PM)
    var keepPatchedApks by booleanPreference("keep_patched_apks", false)
}
