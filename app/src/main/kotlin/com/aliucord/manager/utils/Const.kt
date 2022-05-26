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
