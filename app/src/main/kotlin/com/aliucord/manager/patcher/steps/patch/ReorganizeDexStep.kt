package com.aliucord.manager.patcher.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReorganizeDexStep : Step(), KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_reorganize_dex

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val dexProviders = container.steps
            .filterIsInstance<IDexProvider>()
            .sortedByDescending { it.dexPriority }
        val priorityDexCount = dexProviders
            .filter { it.dexPriority > 0 }
            .sumOf { it.dexCount }

        var dexCount = 0

        ZipReader(apk).use { zip ->
            // Count the amount of dex files currently in the apk
            dexCount = zip.entryNames.count { it.endsWith(".dex") }

            // Copy all the dex files that need to be moved out of the apk
            for (idx in 0..<priorityDexCount) {
                // Not enough dex files to move
                if (idx + 1 > dexCount) break

                val bytes = zip.openEntry(getDexName(idx))!!.read()
                val file = paths.patchingWorkingDir().resolve(getDexName(idx))
                file.writeBytes(bytes)
            }
        }

        ZipWriter(apk, /* append = */ true).use { zip ->
            // Delete all the old dex files from the apk
            for (idx in 0..<priorityDexCount) {
                // Not enough dex files to move
                if (idx + 1 > dexCount) break

                zip.deleteEntry(getDexName(idx))
            }

            // Copy all of the high priority dex files to the apk
            var idx = 0
            for (dexProvider in dexProviders) {
                if (dexProvider.dexPriority <= 0) continue

                for (dexBytes in dexProvider.getDexFiles()) {
                    zip.writeEntry(getDexName(idx++), dexBytes)
                }
            }

            // Copy back the dex files that were moved out of the apk
            for (idx in 0..<priorityDexCount) {
                // Not enough dex files to move
                if (idx + 1 > dexCount) break

                val file = paths.patchingWorkingDir().resolve(getDexName(idx))
                val bytes = file.readBytes()
                zip.writeEntry(getDexName(dexCount + idx), bytes)
            }

            dexCount += idx

            // Copy the rest of the injected dex files
            for (dexProvider in dexProviders) {
                if (dexProvider.dexPriority > 0) continue

                for (dexBytes in dexProvider.getDexFiles()) {
                    zip.writeEntry(getDexName(dexCount++), dexBytes)
                }
            }
        }
    }

    private fun getDexName(idx: Int) = "classes${if (idx == 0) "" else (idx + 1)}.dex"
}
