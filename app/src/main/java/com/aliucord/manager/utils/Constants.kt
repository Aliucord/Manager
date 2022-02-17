package com.aliucord.manager.utils

import android.os.Environment
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.android.*
import java.io.File

val httpClient = HttpClient(Android)
val aliucordDir = File(Environment.getExternalStorageDirectory(), "Aliucord")
val pluginsDir = File(aliucordDir, "plugins")
val gson = Gson()