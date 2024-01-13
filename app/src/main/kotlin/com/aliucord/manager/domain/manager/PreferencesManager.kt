package com.aliucord.manager.domain.manager

import android.content.SharedPreferences
import com.aliucord.manager.domain.manager.base.BasePreferenceManager
import com.aliucord.manager.ui.components.Theme

class PreferencesManager(preferences: SharedPreferences) : BasePreferenceManager(preferences) {
    var theme by enumPreference("theme", Theme.SYSTEM)
    var dynamicColor by booleanPreference("dynamic_color", true)
    var replaceIcon by booleanPreference("replace_icon", true)
    var devMode by booleanPreference("dev_mode", false)
    var debuggable by booleanPreference("debuggable", false)
    var appName by stringPreference("app_name", "Aliucord")
    var packageName by stringPreference("package_name", "com.aliucord")
    var version by stringPreference("version", "146108")
    var hermesReplaceLibCpp by booleanPreference("hermes_replace_libcpp", false)
    var keepPatchedApks by booleanPreference("keep_patched_apks", false)
}
