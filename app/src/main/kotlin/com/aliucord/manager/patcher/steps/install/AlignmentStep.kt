package com.aliucord.manager.patcher.steps.install

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Align certain files in the APK to the necessary boundaries.
 */
class AlignmentStep : Step(), KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_alignment

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        var resourcesArscBytes: ByteArray? = null
        var nativeLibPaths: List<String>? = null
        var dexCount: Int = -1

        // Align resources.arsc due to targeting API 30 for silent install
        if (Build.VERSION.SDK_INT >= 30) {
            resourcesArscBytes = ZipReader(apk)
                .use { it.openEntry("resources.arsc")?.read() }
                ?: throw IllegalArgumentException("APK is missing resources.arsc")
        }

        // Align dex files due to using useEmbeddedDex (ref. ManifestPatcher)
        if (Build.VERSION.SDK_INT >= 29) {
            ZipReader(apk).use { zip ->
                // Count the amount of dex files currently in the apk
                dexCount = zip.entryNames.count { it.endsWith(".dex") }

                // Copy all the dex files that need to be moved out of the apk
                for (idx in 0..<dexCount) {
                    val bytes = zip.openEntry(getDexName(idx))!!.read()
                    val file = paths.patchingWorkingDir().resolve(getDexName(idx))
                    file.writeBytes(bytes)
                }
            }
        }

        // Align native libs due to using extractNativeLibs
        nativeLibPaths = ZipReader(apk).use { zip ->
            val libPaths = zip.entryNames.filter { it.endsWith(".so") }

            // Extract to disk temporarily
            for ((idx, path) in libPaths.withIndex()) {
                // Ignore lib architectures that don't match this device
                if (!path.startsWith("lib/$currentDeviceArch"))
                    continue

                // Index is just used as a placeholder id to cache on disk
                val bytes = zip.openEntry(path)!!.read()
                val file = paths.patchingWorkingDir().resolve("$idx.so")
                file.writeBytes(bytes)
            }

            libPaths
        }

        ZipWriter(apk, /* append = */ true).use { zip ->
            // Delete all the unaligned files from APK
            if (resourcesArscBytes != null)
                zip.deleteEntry("resources.arsc")

            for (i in 0..<dexCount)
                zip.deleteEntry(getDexName(i))

            for (path in nativeLibPaths)
                zip.deleteEntry(path)

            // Write all the files back aligned this time
            if (resourcesArscBytes != null)
                zip.writeEntry("resources.arsc", resourcesArscBytes, ZipCompression.NONE, 4)

            for (idx in 0..<dexCount) {
                val file = paths.patchingWorkingDir().resolve(getDexName(idx))
                val bytes = file.readBytes()
                zip.writeEntry(getDexName(idx), bytes, ZipCompression.NONE, 4)
            }

            // Write back native libraries aligned to 16KiB page boundary
            for ((idx, path) in nativeLibPaths.withIndex()) {
                // Ignore lib architectures that don't match this device
                if (!path.startsWith("lib/$currentDeviceArch"))
                    continue

                val file = paths.patchingWorkingDir().resolve("$idx.so")
                val bytes = file.readBytes()
                zip.writeEntry(path, bytes, ZipCompression.NONE, 16384)
            }
        }
    }

    private fun getDexName(idx: Int) = "classes${if (idx == 0) "" else (idx + 1)}.dex"
}
