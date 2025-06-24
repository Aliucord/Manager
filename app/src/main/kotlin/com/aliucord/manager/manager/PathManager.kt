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
     * Delete all the cache dirs and recreate them.
     */
    fun clearCache() {
        context.cacheDir.apply { deleteRecursively() && mkdirs() }
        patchingDir.apply { deleteRecursively() && mkdirs() }
    }

    /**
     * Create a new subfolder in the Discord APK cache for a specific version.
     */
    fun discordApkVersionCache(version: Int): File = patchingDir
        .resolve("discord")
        .resolve(version.toString())
        .apply { mkdirs() }

    /**
     * Resolve a specific path for a cached injector.
     */
    fun cachedInjectorDex(version: SemVer, custom: Boolean = false) = patchingDir
        .resolve("injector").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.dex")

    /**
     * Get all the versions of custom injector builds.
     */
    fun customInjectorDexs() = listCustomFiles(patchingDir.resolve("injector"))

    /**
     * Resolve a specific path for a versioned cached Aliuhook build
     */
    fun cachedAliuhookAAR(version: SemVer) = patchingDir
        .resolve("aliuhook").apply { mkdirs() }
        .resolve("$version.aar")

    /**
     * Resolve a specific path for a versioned smali patches archive.
     */
    fun cachedSmaliPatches(version: SemVer, custom: Boolean = false) = patchingDir
        .resolve("patches").apply { mkdirs() }
        .resolve("$version${if (custom) ".custom" else ""}.zip")

    /**
     * Get all the versions of custom smali bundles.
     */
    fun customSmaliPatches() = listCustomFiles(patchingDir.resolve("patches"))

    /**
     * Singular Kotlin file of the most up-to-date version
     * since the stdlib is backwards compatible.
     */
    fun cachedKotlinDex() = patchingDir
        .resolve("kotlin.dex")

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
         * List all the files that follow the ```[SemVer].custom.*``` naming scheme.
         */
        private fun listCustomFiles(dir: File): List<SemVer>? {
            if (!dir.exists()) return null

            val files = dir.listFiles()
                ?: throw Error("Failed to list directory")

            val customVersions = files
                .map { it.nameWithoutExtension }
                .filter { it.endsWith(".custom") }
                .map { it.removeSuffix(".custom") }

            return customVersions.mapNotNull(SemVer::parseOrNull)
        }
    }
}
