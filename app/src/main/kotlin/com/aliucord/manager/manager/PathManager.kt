package com.aliucord.manager.manager

import android.content.Context
import android.os.Environment
import com.aliucord.manager.network.utils.SemVer
import java.io.File

/**
 * A central place to provide all system paths that are used.
 */
class PathManager(
    private val context: Context,
    private val prefs: PreferencesManager,
) {
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

    /**
     * Uses the external cache directory (`/storage/emulated/0/Android/data/com.aliucord.manager/cache`)
     * when dev mode or preserving APKs is enabled. Otherwise, default to internal app cache.
     */
    val cacheDir: File
        get() = when (prefs.devMode || prefs.keepPatchedApks) {
            false -> context.cacheDir
            true -> context.externalCacheDir ?: context.cacheDir
        }

    /**
     * Delete the entire cache dir and recreate it.
     */
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    /**
     * Create a new subfolder in the Discord APK cache for a specific version.
     */
    fun discordApkVersionCache(version: Int): File = cacheDir
        .resolve("discord")
        .resolve(version.toString())
        .apply { mkdirs() }

    /**
     * Resolve a specific path for a cached injector.
     */
    fun cachedInjectorDex(version: SemVer, custom: Boolean = false) = cacheDir
        .resolve("injector").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.dex")

    /**
     * Get all the versions of custom injector builds.
     */
    fun customInjectorDexs() = listCustomFiles(cacheDir.resolve("injector"))

    /**
     * Resolve a specific path for a versioned cached Aliuhook build
     */
    fun cachedAliuhookAAR(version: SemVer) = cacheDir
        .resolve("aliuhook").apply { mkdirs() }
        .resolve("$version.aar")

    /**
     * Resolve a specific path for a versioned smali patches archive.
     */
    fun cachedSmaliPatches(version: SemVer, custom: Boolean = false) = cacheDir
        .resolve("patches").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.zip")

    /**
     * Get all the versions of custom smali bundles.
     */
    fun customSmaliPatches() = listCustomFiles(cacheDir.resolve("patches"))

    /**
     * Singular Kotlin file of the most up-to-date version
     * since the stdlib is backwards compatible.
     */
    fun cachedKotlinDex() = cacheDir
        .resolve("kotlin.dex")

    /**
     * The temporary working directory of a currently executing patching process.
     */
    fun patchingWorkingDir() = cacheDir
        .resolve("patched")

    private companion object {
        /**
         * List all the files that follow the ```[SemVer].custom.*``` naming scheme.
         */
        private fun listCustomFiles(dir: File): List<SemVer> {
            val files = dir.listFiles() ?: return emptyList()
            val customVersions = files
                .map { it.nameWithoutExtension }
                .filter { it.endsWith(".custom") }
                .map { it.removeSuffix(".custom") }

            return customVersions.mapNotNull(SemVer::parseOrNull)
        }
    }
}
