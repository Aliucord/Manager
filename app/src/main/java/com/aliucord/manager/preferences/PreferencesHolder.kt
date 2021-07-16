/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.preferences

import android.os.Environment

val themePreference = intPreference("theme")
val replaceBgPreference = boolPreference("replace_bg", true)
val useDexFromStoragePreference = boolPreference("use_dex_from_storage")
val dexLocationPreference = stringPreference("dex_location", Environment.getExternalStorageDirectory().absolutePath + "/Aliucord/Aliucord.dex")
