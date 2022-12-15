/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

private val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
@Immutable
data class Plugin(val file: File) {
    val manifest: Manifest

    init {
        ZipFile(file).use { zipFile ->
            val entry = zipFile.getEntry("manifest.json")
                ?: throw ZipException("Plugin ${file.nameWithoutExtension} has no manifest.")

            manifest = zipFile.getInputStream(entry).use { zis -> json.decodeFromStream(zis) }
        }
    }
}
