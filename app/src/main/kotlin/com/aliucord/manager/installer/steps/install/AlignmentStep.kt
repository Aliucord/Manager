package com.aliucord.manager.installer.steps.install

import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.installer.steps.patch.CopyDependenciesStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent

/**
 * Align certain files in the APK to a 4KiB boundary.
 */
class AlignmentStep : Step(), KoinComponent {
    private val currentDeviceArch = Build.SUPPORTED_ABIS.first()

    override val group = StepGroup.Install
    override val localizedName = R.string.install_step_alignment

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        // Align resources.arsc due to targeting API 30 for silent install
        if (Build.VERSION.SDK_INT >= 30) {
            val bytes = ZipReader(apk)
                .use { it.openEntry("resources.arsc")?.read() }
                ?: throw IllegalArgumentException("APK is missing resources.arsc")

            ZipWriter(apk, /* append = */ true).use {
                it.deleteEntry("resources.arsc")
                it.writeEntry("resources.arsc", bytes, ZipCompression.NONE, 4096)
            }
        } else {
            state = StepState.Skipped
        }
    }
}
