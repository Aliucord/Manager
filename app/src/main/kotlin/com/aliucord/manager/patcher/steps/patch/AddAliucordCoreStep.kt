package com.aliucord.manager.patcher.steps.patch

import android.content.Context
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Embed the Aliucord core (with Sunflower) into the patched APK so the install is standalone
 *
 * The bundled core is located at: assets/sunflower/Aliucord.ziz
 */
class AddAliucordCoreStep : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_add_core

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().apk

        val coreBytes = try {
            context.assets.open("sunflower/Aliucord.zip").use { it.readBytes() }
        } catch (e: Exception) {
            container.log("No bundled Aliucord core in Manager assets; using default")
            return
        }

        val apkEntryPath = "Aliucord.zip"
        val exists = ZipReader(apk).use { apkEntryPath in it.entryNames }

        ZipWriter(apk, true).use { zip ->
            if (exists) zip.deleteEntry(apkEntryPath)
            zip.writeEntry(apkEntryPath, coreBytes, ZipCompression.NONE)
        }
    }
}
