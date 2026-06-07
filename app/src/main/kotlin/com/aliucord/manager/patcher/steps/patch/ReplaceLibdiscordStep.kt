package com.aliucord.manager.patcher.steps.patch

import android.content.Context
import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Replace Discord's bundled libdiscord.so with the one from 333.5 (333205),
 * which Sunflower's Kotlin media-engine layer.
 *
 * The bundled libs are located at:
 * assets/sunflower/<arch>/libdiscord.so
 *
 * Written uncompressed + unaligned here; [com.aliucord.manager.patcher.steps.install.AlignmentStep] re-aligns all .so to 16KiB.
 */
class ReplaceLibdiscordStep : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_replace_libdiscord

    override suspend fun execute(container: StepRunner) {
        val arch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk

        val assetPath = "sunflower/$arch/libdiscord.so"
        val newBytes = try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            container.log("No bundled Sunflower libdiscord.so for arch $arch; leaving original engine in place")
            return
        }

        val apkLibPath = "lib/$arch/libdiscord.so"
        val exists = ZipReader(apk).use { apkLibPath in it.entryNames }
        container.log("Replacing $apkLibPath with Sunflower 333.5 engine (${newBytes.size} bytes); existing=$exists")

        ZipWriter(apk, true).use { zip ->
            if (exists) zip.deleteEntry(apkLibPath)
            zip.writeEntry(apkLibPath, newBytes, ZipCompression.NONE)
        }
    }
}
