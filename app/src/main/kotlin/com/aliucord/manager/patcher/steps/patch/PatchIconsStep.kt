package com.aliucord.manager.patcher.steps.patch

import android.content.Context
import android.graphics.*
import android.graphics.drawable.InsetDrawable
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
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
import com.aliucord.manager.patcher.util.ArscUtil.getResourceFileNames
import com.aliucord.manager.patcher.util.AxmlUtil
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement
import com.aliucord.manager.util.getRawBytes
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Patch the mipmap-v26 launcher icons' background, foreground and monochrome attributes,
 * and if below API 26, replace the traditional mipmap icons instead.
 * All filenames are retrieved based on the resource definitions in `resources.arsc`,
 * so that this works on pretty much almost any APK.
 */
@Stable
class PatchIconsStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_icon

    // Adaptive icons are available starting with Android 8
    private val isAdaptiveIconsAvailable = Build.VERSION.SDK_INT >= 26

    // Monochrome icons are always patched starting with Android 12
    private val isMonochromeIconsAvailable = Build.VERSION.SDK_INT >= 31

    override suspend fun execute(container: StepRunner) {
        container.log("isAdaptiveIconsAvailable: $isAdaptiveIconsAvailable, isMonochromeIconsAvailable: $isMonochromeIconsAvailable")

        // Skip if not patching main icon AND monochrome icons are unavailable
        if (!isMonochromeIconsAvailable && options.iconReplacement is IconReplacement.Original) {
            container.log("No patching necessary, skipping step")
            state = StepState.Skipped
            return
        }

        container.log("Parsing resources.arsc")
        val apk = container.getStep<CopyDependenciesStep>().apk
        val arsc = ArscUtil.readArsc(apk)

        if (isAdaptiveIconsAvailable) {
            patchAdaptiveIcons(
                container = container,
                apk = apk,
                arsc = arsc,
            )
        } else {
            patchRawIcons(
                container = container,
                apk = apk,
                arsc = arsc,
            )
        }
    }

    private fun patchAdaptiveIcons(
        container: StepRunner,
        apk: File,
        arsc: BinaryResourceFile,
    ) {
        container.log("Parsing AndroidManifest.xml and obtaining adaptive square/round icon file paths")
        val iconResourceIds = AxmlUtil.readManifestIconInfo(apk)
        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(
            resourceId = iconResourceIds.squareIcon,
            configurationName = "anydpi-v26"
        )
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(
            resourceId = iconResourceIds.roundIcon,
            configurationName = "anydpi-v26"
        )

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

        // Add a new color and axml svg resource
        else if (options.iconReplacement is IconReplacement.OldDiscord) {
            container.log("Adding icon color resource to arsc")
            backgroundIcon = arsc.getPackageChunk()
                .addColorResource("icon_background_replacement", IconReplacement.OldBlurpleColor)

            container.log("Adding custom icon foreground to arsc")

            val iconPathIdx = arsc.getMainArscChunk().stringPool
                .addString("res/ic_foreground_replacement.xml")

            foregroundIcon = arsc.getPackageChunk().addResource(
                typeName = "drawable",
                resourceName = "ic_foreground_replacement",
                configurations = { it.toString() == "anydpi" },
                valueType = BinaryResourceValue.Type.STRING,
                valueData = iconPathIdx,
            )
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
                val monochromeIconId = if (options.iconReplacement is IconReplacement.OldDiscord) {
                    R.drawable.ic_discord_old_monochrome
                } else {
                    R.drawable.ic_discord_monochrome
                }

                container.log("Writing monochrome icon AXML to apk")
                it.writeEntry("res/ic_aliucord_monochrome.xml", context.resources.getRawBytes(monochromeIconId))
            }

            if (options.iconReplacement is IconReplacement.OldDiscord) {
                container.log("Writing custom icon foreground to apk")
                it.writeEntry("res/ic_foreground_replacement.xml", context.resources.getRawBytes(R.drawable.ic_discord_old_monochrome))
            } else if (options.iconReplacement is IconReplacement.CustomImage) {
                container.log("Writing custom icon foreground to apk")
                it.writeEntry("res/ic_foreground_replacement.png", options.iconReplacement.imageBytes)
            }

            container.log("Writing resources unaligned compressed")
            it.deleteEntry("resources.arsc")
            // This doesn't need to be aligned and uncompressed here, since that is done during AlignmentStep
            it.writeEntry("resources.arsc", arsc.toByteArray())
        }
    }

    private fun patchRawIcons(
        container: StepRunner,
        apk: File,
        arsc: BinaryResourceFile,
    ) {
        container.log("Parsing AndroidManifest.xml and obtaining all square/round launcher icon file paths")
        val iconResourceIds = AxmlUtil.readManifestIconInfo(apk)
        val squareIconFiles = arsc.getMainArscChunk().getResourceFileNames(
            resourceId = iconResourceIds.squareIcon,
            configurations = { it.toString() != "anydpi-v26" },
        )
        val roundIconFiles = arsc.getMainArscChunk().getResourceFileNames(
            resourceId = iconResourceIds.roundIcon,
            configurations = { it.toString() != "anydpi-v26" },
        )
        val allIconFiles = squareIconFiles + roundIconFiles

        val backgroundColor = when (options.iconReplacement) {
            IconReplacement.Original -> IconReplacement.AliucordColor
            IconReplacement.OldDiscord -> IconReplacement.OldBlurpleColor
            is IconReplacement.CustomColor -> options.iconReplacement.color
            is IconReplacement.CustomImage -> error("Cannot patch custom images below Android 8 (Adaptive Icons are required)")
        }

        container.log("Generating static launcher icon")
        val icon = createDiscordLauncherIcon(
            backgroundColor = backgroundColor,
            oldLogo = options.iconReplacement == IconReplacement.OldDiscord,
        )
        val iconBytes = ByteArrayOutputStream().use {
            icon.compress(Bitmap.CompressFormat.PNG, 0, it)
            icon.recycle()
            it.toByteArray()
        }

        container.log("Writing patched icons back to apk")
        ZipWriter(apk, /* append = */ true).use {
            it.deleteEntries(allIconFiles)

            for (path in allIconFiles)
                it.writeEntry(path, iconBytes)
        }
    }

    /**
     * Draws the Discord icon over a custom colored background and rounds it
     * to create a launcher icon like Discord's. This is used for devices that
     * do not support adaptive icons.
     */
    private fun createDiscordLauncherIcon(
        backgroundColor: Color,
        oldLogo: Boolean = false,
        size: Int = 192,
    ): Bitmap {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            setColor(backgroundColor.toArgb())
        }

        val drawableId = when (oldLogo) {
            false -> R.drawable.ic_discord
            true -> R.drawable.ic_discord_old
        }
        val vectorDrawable = ContextCompat.getDrawable(context, drawableId)!!
        val drawable = InsetDrawable(vectorDrawable, (size * .17f).toInt()).apply {
            setTint(Color.White.toArgb())
        }

        val icon = createBitmap(size, size).applyCanvas {
            drawRect(Rect(0, 0, width, height), paint)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
        val iconRound = RoundedBitmapDrawableFactory.create(context.resources, icon).apply {
            isCircular = true
            setAntiAlias(true)
        }.toBitmap()

        icon.recycle()
        return iconRound
    }
}
