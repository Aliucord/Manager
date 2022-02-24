/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.preferences

import android.os.Environment

var themePref = intPreference("theme")
val replaceBg = boolPreference("replace_bg", true)
val devModePreference = boolPreference("dev_mode", false)
val debuggablePreference = boolPreference("debuggable")
val useDexFromStoragePreference = boolPreference("use_dex_from_storage")
val dexLocationPreference = stringPreference("dex_location", Environment.getExternalStorageDirectory().absolutePath + "/Aliucord/Injector.dex")

object Preferences {
    val useBlack = boolPreference("use_black")
}