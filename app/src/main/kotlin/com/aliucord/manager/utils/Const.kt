/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import android.os.Environment
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*

const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
const val REQUEST_LEGACY_STORAGE = "requestLegacyExternalStorage"
const val DEBUGGABLE = "debuggable"

val httpClient = HttpClient(Android) {
    install(JsonFeature)
}
val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")
val pluginsDir = aliucordDir.resolve("plugins")
val gson = Gson()
