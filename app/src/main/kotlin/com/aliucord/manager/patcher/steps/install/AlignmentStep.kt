package com.aliucord.manager.patcher.steps.install

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Align certain files in the APK to the respective boundaries.
 */
class AlignmentStep : Step(), KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_alignment

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        if (Build.VERSION.SDK_INT < 29) {
            state = StepState.Skipped
            return
        }

        var resourcesArscBytes: ByteArray? = null
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

        ZipWriter(apk, /* append = */ true).use { zip ->
            // Delete all the unaligned files from APK
            if (resourcesArscBytes != null)
                zip.deleteEntry("resources.arsc")

            for (i in 0..<dexCount)
                zip.deleteEntry(getDexName(i))

            // Write all the files back aligned this time
            if (resourcesArscBytes != null)
                zip.writeEntry("resources.arsc", resourcesArscBytes, ZipCompression.NONE, 4)

            for (idx in 0..<dexCount) {
                val file = paths.patchingWorkingDir().resolve(getDexName(idx))
                val bytes = file.readBytes()
                zip.writeEntry(getDexName(idx), bytes, ZipCompression.NONE, 4)
            }
        }
    }

    private fun getDexName(idx: Int) = "classes${if (idx == 0) "" else (idx + 1)}.dex"
}
