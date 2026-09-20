package com.aliucord.manager.patcher.steps.patch

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.steps.download.DownloadVoiceEngineStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent


/**
 * Replace Discord's bundled libdiscord.so with the one from 333.5 (333205),
 * which Sunflower's Kotlin media-engine layer.
 */
class PatchVoiceEngineStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_replace_voice_engine

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk
        val libApk = container.getStep<DownloadVoiceEngineStep>().getStoredFile(container)

        val libBytes = ZipReader(libApk)
            .use { it.openEntry("lib/$currentDeviceArch/libdiscord.so")?.read() }
            ?: throw IllegalStateException("New libdiscord.so not found in split lib apk!")

        val apkLibPath = "lib/$currentDeviceArch/libdiscord.so"

        container.log("Replacing old libdiscord.so")
        ZipWriter(apk, true).use { zip ->
            zip.deleteEntry(apkLibPath)
            zip.writeEntry(apkLibPath, libBytes, ZipCompression.NONE) // Alignment done later
        }
    }
}
