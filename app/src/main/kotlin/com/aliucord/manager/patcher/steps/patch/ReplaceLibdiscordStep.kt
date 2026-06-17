package com.aliucord.manager.patcher.steps.patch

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.steps.download.DownloadAliuvoiceStep
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
        val arch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk
        val aliuvoice = container.getStep<DownloadAliuvoiceStep>().getStoredFile(container)

        val libs = ZipReader(aliuvoice).use { aar ->
            aar.entryNames
                .filter { it.startsWith("jni/$arch/") && it.endsWith(".so") }
                .associate { it.substringAfterLast('/') to aar.openEntry(it)!!.read() }
        }

        if (libs.isEmpty()) {
            container.log("No Aliuvoice libs for arch $arch; leaving original engine in place")
            return
        }

        val existing = ZipReader(apk).use { it.entryNames.toHashSet() }

        ZipWriter(apk, true).use { zip ->
            for ((name, bytes) in libs) {
                val apkLibPath = "lib/$arch/$name"
                container.log("Writing $apkLibPath from Aliuvoice (${bytes.size} bytes); existing=${apkLibPath in existing}")
                if (apkLibPath in existing) zip.deleteEntry(apkLibPath)
                zip.writeEntry(apkLibPath, bytes, ZipCompression.NONE)
            }
        }
    }
}
