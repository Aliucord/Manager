package com.aliucord.manager.installer.steps.patch

import android.content.Context
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream

/**
 * Replace icons
 */
@Stable
class ReplaceIconStep(private val options: InstallOptions) : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_patch_icon

    override suspend fun execute(container: StepRunner) {
        if (!options.replaceIcon) {
            state = StepState.Skipped
            return
        }

        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        ZipWriter(apk, /* append = */ true).use {
            val foregroundIcon = readAsset("icons/ic_logo_foreground.png")
            val squareIcon = readAsset("icons/ic_logo_square.png")

            val replacements = mapOf(
                arrayOf("MbV.png", "kbF.png", "_eu.png", "EtS.png") to foregroundIcon,
                arrayOf("_h_.png", "9MB.png", "Dy7.png", "kC0.png", "oEH.png", "RG0.png", "ud_.png", "W_3.png") to squareIcon
            )

            for ((files, replacement) in replacements) {
                for (file in files) {
                    val path = "res/$file"
                    it.deleteEntry(path)
                    it.writeEntry(path, replacement)
                }
            }
        }
    }

    private fun readAsset(fileName: String): ByteArray =
        context.assets.open(fileName).use(InputStream::readBytes)
}
