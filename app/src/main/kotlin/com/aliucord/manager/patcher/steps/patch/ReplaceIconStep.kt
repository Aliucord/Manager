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
        container.log("isAdaptiveIconsAvailable: $isAdaptiveIconsAvailable, isMonochromeIconsAvailable: $isMonochromeIconsAvailable")

        // Adaptive icons are available starting with Android 8
        // Monochrome icons are always patched starting with Android 12
        // Skip if adaptive icons are unavailable or { not patching main icon AND monochrome icons are unavailable }
        if (!isAdaptiveIconsAvailable || (!isMonochromeIconsAvailable && options.iconReplacement is IconReplacement.Original)) {
            container.log("No patching necessary, skipping step")
            state = StepState.Skipped
            return
        }

        container.log("Parsing resources.arsc")
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val arsc = ArscUtil.readArsc(apk)

        container.log("Parsing AndroidManifest.xml and obtaining adaptive square/round icon file paths")
        val iconRscIds = AxmlUtil.readManifestIconInfo(apk)
        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

        var foregroundIcon: BinaryResourceIdentifier? = null
        var backgroundIcon: BinaryResourceIdentifier? = null
        var monochromeIcon: BinaryResourceIdentifier? = null

        // Add the monochrome resource and add the resource file later
        if (isMonochromeIconsAvailable) {
            container.log("Adding monochrome icon resource to arsc")

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
            container.log("Adding icon color resource to arsc")

            backgroundIcon = arsc.getPackageChunk()
                .addColorResource("icon_background_replacement", options.iconReplacement.color)
        }

        // Add a new mipmap resource and the file to be added later
        else if (options.iconReplacement is IconReplacement.CustomImage) {
            container.log("Adding custom icon foreground to arsc")

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
            container.log("Patching and writing adaptive icon AXML at $rscFile")
            AxmlUtil.patchAdaptiveIcon(
                apk = apk,
                resourcePath = rscFile,
                backgroundColor = backgroundIcon,
                foregroundIcon = foregroundIcon,
                monochromeIcon = monochromeIcon,
            )
        }

        container.log("Writing other patched files back to apk")
        ZipWriter(apk, /* append = */ true).use {
            if (isMonochromeIconsAvailable) {
                container.log("Writing monochrome icon AXML to apk")
                it.writeEntry("res/ic_aliucord_monochrome.xml", context.getResBytes(R.drawable.ic_discord_monochrome))
            }

            if (options.iconReplacement is IconReplacement.CustomImage) {
                container.log("Writing custom icon foreground to apk")
                it.writeEntry("res/ic_foreground_replacement.png", options.iconReplacement.imageBytes)
            }

            container.log("Writing resources unaligned compressed")
            it.deleteEntry("resources.arsc")
            // This doesn't need to be aligned and uncompressed here, since it that is done during AlignmentSte
            it.writeEntry("resources.arsc", arsc.toByteArray())
        }
    }
}
