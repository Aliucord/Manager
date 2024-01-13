/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.domain.model

import androidx.compose.runtime.Immutable
import com.github.diamondminer88.zip.ZipReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

@Immutable
data class Plugin(val file: File) : KoinComponent {
    val manifest: Manifest

    init {
        ZipReader(file).use {
            val manifest = it.openEntry("manifest.json")
                ?: throw Exception("Plugin ${file.nameWithoutExtension} has no manifest.")

            @OptIn(ExperimentalSerializationApi::class)
            this.manifest = get<Json>().decodeFromStream(manifest.read().inputStream())
        }
    }
}
