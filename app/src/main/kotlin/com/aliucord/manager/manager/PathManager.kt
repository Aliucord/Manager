package com.aliucord.manager.manager

import android.content.Context
import android.os.Environment
import com.aliucord.manager.network.utils.SemVer
import java.io.File

/**
 * A central place to provide all system paths that are used.
 */
class PathManager(context: Context) {
    /**
     * The Aliucord folder in which plugins/settings/themes are stored.
     * Standard path: `~/Aliucord`
     */
    val aliucordDir = Environment.getExternalStorageDirectory().resolve("Aliucord")

    /**
     * The directory in external storage in which plugins are stored by Aliucord.
     */
    val pluginsDir = aliucordDir.resolve("plugins")

    /**
     * The settings file in which Aliucord's core uses.
     */
    val coreSettingsFile = aliucordDir.resolve("settings/Aliucord.json")

    /**
     * Global keystore used for signing APKs.
     */
    val keystoreFile = aliucordDir.resolve("ks.keystore")

    private val externalCacheDir = context.externalCacheDir
        ?: throw Error("External cache directory isn't supported")

    /**
     * Standard path: `~/Android/data/com.aliucord.manager/cache`
     */
    private val discordApkCache = externalCacheDir
        .resolve("discord")

    /**
     * Delete the entire cache dir and recreate it.
     */
    fun clearCache() {
        if (!externalCacheDir.deleteRecursively())
            throw IllegalStateException("Failed to delete cache")

        externalCacheDir.mkdirs()
    }

    /**
     * Create a new subfolder in the Discord APK cache for a specific version.
     */
    fun discordApkVersionCache(version: Int): File = discordApkCache
        .resolve(version.toString())
        .apply { mkdirs() }

    /**
     * Resolve a specific path for a cached injector.
     */
    fun cachedInjectorDex(aliucordHash: SemVer) = externalCacheDir
        .resolve("injector").apply { mkdirs() }
        .resolve("$aliucordHash.dex")

    /**
     * Resolve a specific path for a versioned cached Aliuhook build
     */
    fun cachedAliuhookAAR(version: String) = externalCacheDir
        .resolve("aliuhook").apply { mkdirs() }
        .resolve("$version.aar")

    /**
     * Resolve a specific path for a versioned smali patches archive.
     */
    fun cachedSmaliPatches(version: SemVer) = externalCacheDir
        .resolve("patches").apply { mkdirs() }
        .resolve("$version.zip")

    /**
     * Singular Kotlin file of the most up-to-date version
     * since the stdlib is backwards compatible.
     */
    fun cachedKotlinDex() = externalCacheDir
        .resolve("kotlin.dex")

    /**
     * The temporary working directory of a currently executing patching process.
     */
    fun patchingWorkingDir() = externalCacheDir
        .resolve("patched")
}
