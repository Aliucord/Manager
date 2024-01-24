package com.aliucord.manager.installer.steps.patch

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.DownloadAliuhookStep
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent

/**
 * Add the Aliuhook library's native libs along with dex
 */
class AddAliuhookStep : Step(), KoinComponent {
    private val currentDeviceArch = Build.SUPPORTED_ABIS.first()

    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_add_aliuhook

    override suspend fun execute(container: StepContainer) {
        val apk = container.getCompletedStep<CopyDependenciesStep>().patchedApk
        val aliuhook = container.getCompletedStep<DownloadAliuhookStep>().targetFile

        // Find the amount of .dex files in the apk
        val dexCount = ZipReader(apk).use {
            it.entryNames.count { name -> name.endsWith(".dex") }
        }

        ZipWriter(apk, /* append = */ true).use { patchedApk ->
            ZipReader(aliuhook).use { aliuhook ->
                for (libFile in arrayOf("libaliuhook.so", "libc++_shared.so", "liblsplant.so")) {
                    val bytes = aliuhook.openEntry("jni/$currentDeviceArch/$libFile")?.read()
                        ?: throw IllegalStateException("Failed to read $libFile from aliuhook aar")

                    patchedApk.writeEntry("lib/$currentDeviceArch/$libFile", bytes)
                }

                val aliuhookDex = aliuhook.openEntry("classes.dex")?.read()
                    ?: throw IllegalStateException("No classes.dex in aliuhook aar")

                patchedApk.writeEntry("classes${dexCount + 1}.dex", aliuhookDex)
            }
        }
    }
}
