package com.aliucord.manager.patcher.steps.patch

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.util.ArscUtil
import com.aliucord.manager.patcher.util.ArscUtil.addColorResource
import com.aliucord.manager.patcher.util.ArscUtil.addResource
import com.aliucord.manager.patcher.util.ArscUtil.getMainArscChunk
import com.aliucord.manager.patcher.util.ArscUtil.getPackageChunk
import com.aliucord.manager.patcher.util.ArscUtil.getResourceFileName
import com.aliucord.manager.patcher.util.AxmlUtil
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement
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
class ReplaceIconStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_icon

    override suspend fun execute(container: StepRunner) {
        val isAdaptiveIconsAvailable = Build.VERSION.SDK_INT >= 28
        val isMonochromeIconsAvailable = Build.VERSION.SDK_INT >= 31

        // Adaptive icons are available starting with Android 8
        // Monochrome icons are always patched starting with Android 12
        // Skip if adaptive icons are unavailable or { not patching main icon AND monochrome icons are unavailable }
        if (!isAdaptiveIconsAvailable || (!isMonochromeIconsAvailable && options.iconReplacement is IconReplacement.Original)) {
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
        if (isMonochromeIconsAvailable) {
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
            if (isMonochromeIconsAvailable) {
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
