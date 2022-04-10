/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.models

import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.utils.json
import com.aliucord.manager.utils.pluginsDir
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

@OptIn(ExperimentalSerializationApi::class)
data class Plugin(val file: File) {
    val manifest: Manifest

    init {
        ZipFile(file).use { zipFile ->
            val entry = zipFile.getEntry("manifest.json")
                ?: throw ZipException("Plugin ${file.nameWithoutExtension} has no manifest.")

            manifest = zipFile.getInputStream(entry).use { zis -> json.decodeFromStream(zis) }
        }
    }

    companion object {
        fun loadAll(): List<Plugin> {
            if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
                Log.e(BuildConfig.TAG, "Failed to create plugins dir. Missing Permissions?")
                return emptyList()
            }

            val files = pluginsDir.listFiles() ?: return emptyList()

            return files.mapNotNull { file ->
                if (file.extension == "zip") {
                    try {
                        Plugin(file)
                    } catch (th: Throwable) {
                        Log.e(BuildConfig.TAG, "Failed to load plugin ${file.nameWithoutExtension}", th)
                        null
                    }
                } else {
                    Log.w(BuildConfig.TAG, "Found non zip ${file.name} in plugins folder.")
                    null
                }
            }.sortedBy { it.manifest.name }
        }
    }
}
