package com.aliucord.manager.manager

import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import com.aliucord.manager.manager.base.BasePreferenceManager
import com.aliucord.manager.ui.theme.Theme

@Stable
class PreferencesManager(preferences: SharedPreferences) : BasePreferenceManager(preferences) {
    var theme by enumPreference("theme", Theme.System)
    var dynamicColor by booleanPreference("dynamic_color", true)
    var devMode by booleanPreference("dev_mode", false)
    var installer by enumPreference<InstallerSetting>("installer", InstallerSetting.PM)
    var keepPatchedApks by booleanPreference("keep_patched_apks", false)
    var showNetworkWarning by booleanPreference("show_network_warning", true)
    var showPlayProtectWarning by booleanPreference("show_play_protect_warning", true)
}
