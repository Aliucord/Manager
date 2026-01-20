package com.aliucord.manager.manager

import android.content.Context
import android.os.Environment
import com.aliucord.manager.network.utils.SemVer
import java.io.File
import java.io.IOException

/**
 * A central place to provide all system paths that are used.
 */
class PathManager(
    private val context: Context,
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
     * The internal directory used for downloading components related to patching, and
     * running the patching process itself.
     *
     * This should not be a cache dir provided by Android, since it will be wiped when
     * the device is low on storage, and result in a failed patching process.
     */
    val patchingDir = context.filesDir.resolve("patching").apply { mkdirs() }

    /**
     * The internal app directory uses for downloads that should not be wiped,
     * to be used during the patching process. When the process completes, then this
     * is to be moved to the cache dir, to allow Android to wipe the downloads when
     * low on storage.
     */
    val patchingDownloadDir = patchingDir.resolve("downloads")

    /**
     * Used as a secondary location for downloads when not currently patching.
     * This allows Android to clear the download cache when low on storage.
     */
    val cacheDownloadDir = context.cacheDir.resolve("downloads")

    /**
     * A permanent location used for storing custom patching components.
     * This does not get moved to an Android-managed cache dir when
     * not currently patching.
     */
    val customComponentsDir = patchingDir.resolve("custom")

    /**
     * Permanent location used for storing custom injectors pushed to the device.
     * No verification for the files placed here is done.
     */
    val customInjectorsDir = customComponentsDir.resolve("injector")

    /**
     * Permanent location used for storing custom smali patch bundles pushed to the device.
     * No verification for the files placed here is done.
     */
    val customPatchesDir = customComponentsDir.resolve("patches")

    /**
     * Delete all the cache dirs and recreate them.
     */
    fun clearCache() {
        for (dir in arrayOf(patchingDir, cacheDownloadDir, context.cacheDir))
            dir.deleteRecursively() && dir.mkdirs()
    }

    /**
     * Create a new subfolder in the Discord APK cache for a specific version and split.
     */
    fun cachedDiscordApk(version: Int, split: String = "base"): File = patchingDownloadDir
        .resolve("discord/$version")
        .apply { mkdirs() }
        .resolve("$split.apk")

    /**
     * Resolve a specific path for a cached injector.
     */
    fun cachedInjector(version: SemVer) = patchingDownloadDir
        .resolve("injector").apply { mkdirs() }
        .resolve("$version.dex")

    /**
     * Get all the versions of custom injector builds.
     */
    fun customInjectors() = customInjectorsDir.listFiles()
        ?.asList()
        ?: throw IOException("Failed to list directory")

    /**
     * Resolve a specific path for a versioned cached Aliuhook build
     */
    fun cachedAliuhookAAR(version: SemVer) = patchingDownloadDir
        .resolve("aliuhook").apply { mkdirs() }
        .resolve("$version.aar")

    /**
     * Resolve a specific path for a versioned smali patches archive.
     */
    fun cachedSmaliPatches(version: SemVer) = patchingDownloadDir
        .resolve("patches").apply { mkdirs() }
        .resolve("$version.zip")

    /**
     * Get all the versions of custom smali bundles.
     */
    fun customSmaliPatches() = customPatchesDir.listFiles()
        ?.asList()
        ?: throw IOException("Failed to list directory")

    /**
     * Resolve a specific path for a versioned Kotlin stdlib dex.
     */
    fun cachedKotlinDex(version: SemVer) = patchingDownloadDir
        .resolve("kotlin-stdlib").apply { mkdirs() }
        .resolve("$version.dex")

    /**
     * The temporary working directory of a currently executing patching process.
     */
    fun patchingWorkingDir() = patchingDir
        .resolve("patched")

    /**
     * The APK that is worked on during the patching process.
     */
    fun patchedApk() = patchingWorkingDir().resolve("patched.apk")
}
