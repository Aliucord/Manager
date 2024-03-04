package com.aliucord.manager.installer.steps.patch

import android.content.Context
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
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
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream

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
        if (!options.monochromeIcon && options.iconReplacement is IconReplacement.Original) {
            state = StepState.Skipped
            return
        }

        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val arsc = ArscUtil.readArsc(apk)

        val iconRscIds = AxmlUtil.readManifestIconInfo(apk)
        val monochromeRscId = if (!options.monochromeIcon) null else {
            val filePathIdx = arsc.getMainArscChunk().stringPool
                .addString("res/ic_aliucord_monochrome.xml")

            arsc.getPackageChunk().addResource(
                typeName = "drawable",
                resourceName = "ic_aliucord_monochrome",
                configurations = { it.isDefault },
                valueType = BinaryResourceValue.Type.STRING,
                valueData = filePathIdx,
            )
        }

        val typeChunks = arsc.getPackageChunk().getTypeChunks("mipmap")
        println("type: mipmap, configs: ${typeChunks.map { it.configuration.toString() }}")

        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

        when (options.iconReplacement) {
            is IconReplacement.Original -> {} // no-op

            is IconReplacement.CustomColor -> {
                val newColorRscId = arsc.getPackageChunk()
                    .addColorResource("aliucord", options.iconReplacement.color)

                for (rscFile in setOf(squareIconFile, roundIconFile)) { // setOf to get rid of duplicates
                    AxmlUtil.patchAdaptiveIcon(
                        apk = apk,
                        resourcePath = rscFile,
                        backgroundColor = newColorRscId,
                        monochromeIcon = monochromeRscId,
                    )
                }
            }

            is IconReplacement.CustomImage -> {}
        }

        val newArscBytes = arsc.toByteArray(/* shrink = */ true)

        ZipWriter(apk, /* append = */ true).use {
            // val squareIcon = readAsset("icons/ic_logo_square.png")
            // val roundIcon = readAsset("icons/ic_logo_round.png")
            //
            // for ((files, replacement) in replacements) {
            //     for (file in files) {
            //         val path = "res/$file"
            //         it.deleteEntry(path)
            //         it.writeEntry(path, replacement)
            //     }
            // }

            if (options.monochromeIcon) {
                it.writeEntry("res/ic_aliucord_monochrome.xml", context.getResBytes(R.drawable.ic_discord_monochrome))
            }

            it.deleteEntry("resources.arsc")
            it.writeEntry("resources.arsc", newArscBytes)
        }
    }

    private fun readAsset(fileName: String): ByteArray =
        context.assets.open(fileName).use(InputStream::readBytes)
}
