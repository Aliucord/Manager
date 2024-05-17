package com.aliucord.manager.installer.steps.patch

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.installer.steps.download.CopyDependenciesStep
import com.aliucord.manager.installer.util.ArscUtil
import com.aliucord.manager.installer.util.ArscUtil.addColorResource
import com.aliucord.manager.installer.util.ArscUtil.addResource
import com.aliucord.manager.installer.util.ArscUtil.getMainArscChunk
import com.aliucord.manager.installer.util.ArscUtil.getPackageChunk
import com.aliucord.manager.installer.util.ArscUtil.getResourceFileName
import com.aliucord.manager.installer.util.AxmlUtil
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.screens.installopts.InstallOptions.IconReplacement
import com.aliucord.manager.util.getResBytes
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceIdentifier
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Patch the mipmap-v26 launcher icons' background, foreground and monochrome attributes,
 * and if below API 26, replace the traditional mipmap icons instead.
 * All filenames are retrieved based on the resource definitions in `resources.arsc`,
 * so that this works on pretty much almost any APK.
 */
@Stable
class ReplaceIconStep(private val options: InstallOptions) : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_patch_icon

    override suspend fun execute(container: StepRunner) {
        if (Build.VERSION.SDK_INT < 26 || (!options.monochromeIcon && options.iconReplacement is IconReplacement.Original)) {
            state = StepState.Skipped
            return
        }

        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val arsc = ArscUtil.readArsc(apk)

        val iconRscIds = AxmlUtil.readManifestIconInfo(apk)
        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

        var foregroundIcon: BinaryResourceIdentifier? = null
        var backgroundIcon: BinaryResourceIdentifier? = null
        var monochromeIcon: BinaryResourceIdentifier? = null

        // Add the monochrome resource and add the resource file later
        if (options.monochromeIcon) {
            val filePathIdx = arsc.getMainArscChunk().stringPool
                .addString("res/ic_aliucord_monochrome.xml")

            monochromeIcon = arsc.getPackageChunk().addResource(
                typeName = "drawable",
                resourceName = "ic_aliucord_monochrome",
                configurations = { it.isDefault },
                valueType = BinaryResourceValue.Type.STRING,
                valueData = filePathIdx,
            )
        }

        // Add a new color resource to use
        if (options.iconReplacement is IconReplacement.CustomColor) {
            backgroundIcon = arsc.getPackageChunk()
                .addColorResource("aliucord", options.iconReplacement.color)
        }

        // Add a new mipmap resource and the file to be added later
        if (options.iconReplacement is IconReplacement.CustomImage) {
            val iconPathIdx = arsc.getMainArscChunk().stringPool
                .addString("res/ic_foreground_replacement.png")

            foregroundIcon = arsc.getPackageChunk().addResource(
                typeName = "mipmap",
                resourceName = "ic_foreground_replacement",
                configurations = { it.toString().endsWith("dpi") }, // Any mipmap config except anydpi-v26
                valueType = BinaryResourceValue.Type.STRING,
                valueData = iconPathIdx,
            )
        }

        for (rscFile in setOf(squareIconFile, roundIconFile)) { // setOf to not possibly patch same file twice
            AxmlUtil.patchAdaptiveIcon(
                apk = apk,
                resourcePath = rscFile,
                backgroundColor = backgroundIcon,
                foregroundIcon = foregroundIcon,
                monochromeIcon = monochromeIcon,
            )
        }

        ZipWriter(apk, /* append = */ true).use {
            if (options.monochromeIcon) {
                it.writeEntry("res/ic_aliucord_monochrome.xml", context.getResBytes(R.drawable.ic_discord_monochrome))
            }

            if (options.iconReplacement is IconReplacement.CustomImage) {
                it.writeEntry("res/ic_foreground_replacement.png", options.iconReplacement.imageBytes)
            }

            it.deleteEntry("resources.arsc")
            it.writeEntry("resources.arsc", arsc.toByteArray())
        }
    }
}
