package com.aliucord.manager.installer.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.DownloadInjectorStep
import com.aliucord.manager.installer.steps.download.DownloadKotlinStep
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent

/**
 * Reorder the existing dex files to add the Aliucord injector as the first `classes.dex` file.
 */
class AddInjectorStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_add_injector

    override suspend fun execute(container: StepContainer) {
        val apk = container.getCompletedStep<CopyDependenciesStep>().patchedApk
        val injector = container.getCompletedStep<DownloadInjectorStep>().targetFile
        val kotlinStdlib = container.getCompletedStep<DownloadKotlinStep>().targetFile

        val (dexCount, firstDexBytes) = ZipReader(apk).use {
            Pair(
                // Find the amount of .dex files in apk
                it.entryNames.count { name -> name.endsWith(".dex") },

                // Get the first dex
                it.openEntry("classes.dex")?.read()
                    ?: throw IllegalStateException("No classes.dex in base apk")
            )
        }

        ZipWriter(apk, /* append = */ true).use {
            // Move copied dex to end of dex list
            it.deleteEntry("classes.dex")
            it.writeEntry("classes${dexCount + 1}.dex", firstDexBytes)

            // Add Kotlin & Aliucord's dex
            it.writeEntry("classes.dex", injector.readBytes())
            it.writeEntry("classes${dexCount + 2}.dex", kotlinStdlib.readBytes())
        }
    }
}
