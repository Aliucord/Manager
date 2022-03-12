package com.aliucord.manager.utils

import android.os.Environment
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import java.io.File

val httpClient = HttpClient(Android) {
    install(JsonFeature)
}
val aliucordDir = File(Environment.getExternalStorageDirectory(), "Aliucord")
val pluginsDir = File(aliucordDir, "plugins")
val gson = Gson()

const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
const val REQUEST_LEGACY_STORAGE = "requestLegacyExternalStorage"
