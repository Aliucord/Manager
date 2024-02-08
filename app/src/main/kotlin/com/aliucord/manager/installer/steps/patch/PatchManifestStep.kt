package com.aliucord.manager.installer.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.util.ManifestPatcher
import com.aliucord.manager.manager.PreferencesManager
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Patch the APK's AndroidManifest.xml
 */
class PatchManifestStep : Step(), KoinComponent {
    private val prefs: PreferencesManager by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_patch_manifests

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        val manifest = ZipReader(apk)
            .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
            ?: throw IllegalArgumentException("No manifest found in APK")

        // val patchedManifest = ManifestPatcher.patchManifest(
        //     manifestBytes = manifest,
        //     packageName = prefs.packageName,
        //     appName = prefs.appName,
        //     debuggable = prefs.debuggable,
        // )
        //
        // ZipWriter(apk, /* append = */ true).use {
        //     it.deleteEntry("AndroidManifest.xml")
        //     it.writeEntry("AndroidManifest.xml", patchedManifest)
        // }
    }
}
