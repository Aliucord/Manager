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
     * Create a new subfolder in the Discord APK cache for a specific version.
     */
    fun discordApkVersionCache(version: Int): File = patchingDownloadDir
        .resolve("discord")
        .resolve(version.toString())
        .apply { mkdirs() }

    /**
     * Resolve a specific path for a cached injector.
     */
    fun cachedInjectorDex(version: SemVer, custom: Boolean = false) = patchingDownloadDir
        .resolve("injector").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.dex")

    /**
     * Get all the versions of custom injector builds.
     */
    fun customInjectorDexs() = listVersionedFiles(customInjectorsDir)

    /**
     * Resolve a specific path for a versioned cached Aliuhook build
     */
    fun cachedAliuhookAAR(version: SemVer) = patchingDownloadDir
        .resolve("aliuhook").apply { mkdirs() }
        .resolve("$version.aar")

    /**
     * Resolve a specific path for a versioned smali patches archive.
     */
    fun cachedSmaliPatches(version: SemVer, custom: Boolean = false) = patchingDownloadDir
        .resolve("patches").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.zip")

    /**
     * Get all the versions of custom smali bundles.
     */
    fun customSmaliPatches() = listVersionedFiles(customPatchesDir)

    /**
     * Resolve a specific path for a versioned Kotlin stdlib dex.
     */
    fun cachedKotlinDex(version: SemVer) = patchingDownloadDir
        .resolve("kotlin").apply { mkdirs() }
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

    private companion object {
        /**
         * List all the files in a directory that follow SemVer naming.
         */
        private fun listVersionedFiles(dir: File): List<SemVer>? {
            if (!dir.exists()) return null

            val files = dir.listFiles()
                ?: throw Error("Failed to list directory")

            return files
                .map { it.nameWithoutExtension }
                .mapNotNull(SemVer::parseOrNull)
        }
    }
}
