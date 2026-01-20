package com.aliucord.manager.patcher.steps.patch

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.steps.download.DownloadAliuhookStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent

/**
 * Add the Aliuhook library's native libs.
 * The dex is handled by [ReorganizeDexStep] through the [IDexProvider] implementation of [DownloadAliuhookStep].
 */
class AddAliuhookLibsStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_add_aliuhook

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val aliuhook = container.getStep<DownloadAliuhookStep>().getStoredFile(container)

        ZipWriter(apk, /* append = */ true).use { patchedApk ->
            ZipReader(aliuhook).use { aliuhook ->
                for (libFile in arrayOf("libaliuhook.so", "libc++_shared.so", "liblsplant.so")) {
                    container.log("Reading aliuhook lib $libFile with arch $currentDeviceArch")

                    val apkLibPath = "lib/$currentDeviceArch/$libFile"
                    val libBytes = aliuhook.openEntry("jni/$currentDeviceArch/$libFile")?.read()
                        ?: throw IllegalStateException("Failed to read $libFile from aliuhook aar")

                    container.log("Writing to $apkLibPath in APK unaligned uncompressed")
                    patchedApk.writeEntry(apkLibPath, libBytes, ZipCompression.NONE)
                }
            }
        }
    }
}
