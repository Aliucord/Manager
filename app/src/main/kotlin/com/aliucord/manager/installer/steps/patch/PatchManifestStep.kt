package com.aliucord.manager.installer.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.CopyDependenciesStep
import com.aliucord.manager.installer.util.ManifestPatcher
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter

/**
 * Patch the APK's AndroidManifest.xml
 */
class PatchManifestStep(private val options: InstallOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_patch_manifests

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        val manifest = ZipReader(apk)
            .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
            ?: throw IllegalArgumentException("No manifest found in APK")

        val patchedManifest = ManifestPatcher.patchManifest(
            manifestBytes = manifest,
            packageName = options.packageName,
            appName = options.appName,
            debuggable = options.debuggable,
        )

        ZipWriter(apk, /* append = */ true).use {
            it.deleteEntry("AndroidManifest.xml")
            it.writeEntry("AndroidManifest.xml", patchedManifest)
        }
    }
}
