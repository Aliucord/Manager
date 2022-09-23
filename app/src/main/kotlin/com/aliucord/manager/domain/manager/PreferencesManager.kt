package com.aliucord.manager.domain.manager

import android.content.SharedPreferences
import com.aliucord.manager.aliucordDir
import com.aliucord.manager.domain.manager.base.BasePreferenceManager
import com.aliucord.manager.ui.theme.Theme

class PreferencesManager(preferences: SharedPreferences) : BasePreferenceManager(preferences) {
    var theme by enumPreference("theme", Theme.SYSTEM)
    var dynamicColor by booleanPreference("dynamic_color", true)
    var replaceBg by booleanPreference("replace_bg", true)
    var devMode by booleanPreference("dev_mode", false)
    var debuggable by booleanPreference("debuggable", false)
    var useDexFromStorage by booleanPreference("use_dex_from_storage", false)
    var dexLocation by stringPreference("dex_location", "$aliucordDir/Injector.dex")
    var appName by stringPreference("app_name", "Aliucord")
    var packageName by stringPreference("package_name", "com.aliucord")
    var version by stringPreference("version", "146108")
}
