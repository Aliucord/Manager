package com.aliucord.manager.patcher.steps.patch

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.steps.download.DownloadLibdiscordStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent


/**
 * Replace Discord's bundled libdiscord.so with the one from 333.5 (333205),
 * which Sunflower's Kotlin media-engine layer.
 *
 * Written uncompressed + unaligned here; [com.aliucord.manager.patcher.steps.install.AlignmentStep] re-aligns all .so to 16KiB.
 */
class ReplaceLibdiscordStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_replace_libdiscord

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk
        val libApk = container.getStep<DownloadLibdiscordStep>().getStoredFile(container)

        val libBytes = ZipReader(libApk).use { it.openEntry("lib/$currentDeviceArch/libdiscord.so")?.read() }

        if (libBytes == null) {
            container.log("No libdiscord.so for arch $currentDeviceArch in split apk; leaving original engine in place")
            return
        }

        val apkLibPath = "lib/$currentDeviceArch/libdiscord.so"
        val existing = ZipReader(apk).use { it.entryNames.toHashSet() }

        ZipWriter(apk, true).use { zip ->
            container.log("Writing $apkLibPath from libdiscord split (${libBytes.size} bytes); existing=${apkLibPath in existing}")
            if (apkLibPath in existing) zip.deleteEntry(apkLibPath)
            zip.writeEntry(apkLibPath, libBytes, ZipCompression.NONE)
        }
    }
}
