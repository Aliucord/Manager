package com.aliucord.manager.utils

import android.util.Log
import com.aliucord.manager.BuildConfig
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipException
import java.util.zip.ZipFile

data class Manifest(
    val name: String,
    val authors: Array<Author>,
    val description: String,
    val version: String,
    val changelog: String?,
    val changelogMedia: String?
) {
    data class Author(val name: String, val id: Long)
}

class Plugin(val file: File) {
    val manifest: Manifest

    init {
        ZipFile(file).use {
            val entry = it.getEntry("manifest.json")
                ?: throw ZipException("Plugin ${file.nameWithoutExtension} has no manifest.")
            it.getInputStream(entry).use { zis ->
                manifest = gson.fromJson(InputStreamReader(zis), Manifest::class.java)
            }
        }
    }

    companion object {
        fun loadAll(): List<Plugin> {
            if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
                Log.e(BuildConfig.TAG, "Failed to create plugins dir. Missing Permissions?")
                return emptyList()
            }

            val files = pluginsDir.listFiles() ?: return emptyList()
            val plugins = ArrayList<Plugin>(files.size)

            files.forEach {
                if (!it.name.endsWith(".zip")) {
                    Log.w(BuildConfig.TAG, "Found non zip ${it.name} in plugins folder.")
                } else {
                    try {
                        plugins.add(Plugin(it))
                    } catch (th: Throwable) {
                        Log.e(BuildConfig.TAG, "Failed to load plugin ${it.nameWithoutExtension}", th)
                    }
                }
            }

            return plugins.apply { sortBy { it.manifest.name }}
        }
    }
}