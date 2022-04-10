/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import android.os.Environment
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
const val REQUEST_LEGACY_STORAGE = "requestLegacyExternalStorage"
const val DEBUGGABLE = "debuggable"

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(json)
    }
}
val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")
val pluginsDir = aliucordDir.resolve("plugins")
