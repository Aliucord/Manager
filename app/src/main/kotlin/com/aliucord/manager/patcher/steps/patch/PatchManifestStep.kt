package com.aliucord.manager.patcher.steps.patch

import android.Manifest
import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.util.AndroidManifestUtil
import com.aliucord.manager.patcher.util.AndroidManifestUtil.ApplicationAttribute
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter

/**
 * Patch the APK's AndroidManifest.xml
 */
class PatchManifestStep(private val options: PatchOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_manifests

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        container.log("Reading manifest from apk")
        val manifestBytes = ZipReader(apk)
            .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
            ?: throw IllegalArgumentException("No manifest found in APK")

        container.log("Patching manifest")
        val manifest = AndroidManifestUtil(manifestBytes)

        if (Build.VERSION.SDK_INT >= 30)
            manifest.addPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

        manifest.addApplicationAttributes(
            // Since we're aligning native libs, there's no need to extract them
            ApplicationAttribute("extractNativeLibs", android.R.attr.extractNativeLibs, false),
            ApplicationAttribute("label", android.R.attr.label, options.appName),
            ApplicationAttribute("debuggable", android.R.attr.debuggable, options.debuggable),
        )

        if (Build.VERSION.SDK_INT >= 29) manifest.addApplicationAttributes(
            // Prevents AOT compilation
            ApplicationAttribute("useEmbeddedDex", android.R.attr.useEmbeddedDex, true),
            // Android 10 storage compatibility(?)
            ApplicationAttribute("requestLegacyExternalStorage", android.R.attr.requestLegacyExternalStorage, true),
        )

        container.log("Writing patched manifest to apk unaligned compressed")
        ZipWriter(apk, /* append = */ true).use {
            it.deleteEntry("AndroidManifest.xml")
            it.writeEntry("AndroidManifest.xml", manifest.toByteArray())
        }
    }
}
