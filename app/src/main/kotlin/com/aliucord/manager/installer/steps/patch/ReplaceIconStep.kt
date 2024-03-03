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
import com.aliucord.manager.installer.util.ArscUtil.getMainArscChunk
import com.aliucord.manager.installer.util.ArscUtil.getPackageChunk
import com.aliucord.manager.installer.util.ArscUtil.getResourceFileName
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.screens.installopts.InstallOptions.IconReplacement
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
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
        if (!options.monochromeIcon && options.iconReplacement is IconReplacement.Original) {
            state = StepState.Skipped
            return
        }

        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val arsc = ArscUtil.readArsc(apk)

        when (options.iconReplacement) {
            is IconReplacement.Original -> {} // no-op

            is IconReplacement.CustomColor -> {
                val iconRscIds = readManifestIconInfo(apk)
                val newColorRscId = arsc.getPackageChunk().addColorResource("aliucord", options.iconReplacement.color)

                val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
                val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

                fun patchIconFile(file: String) {
                    val iconXml = run {
                        ZipReader(apk)
                            .use { it.openEntry(file)!!.read() }
                            .let(::BinaryResourceFile)
                    }

                    val mainXmlChunk = (iconXml.chunks.single() as XmlChunk)
                    val backgroundChunk = mainXmlChunk.chunks.values
                        .filterIsInstance<XmlStartElementChunk>()
                        .find { it.name == "background" }!!
                    val drawableAttr = backgroundChunk.attributes.find { it.name() == "drawable" }!!

                    assert(drawableAttr.typedValue().type() == BinaryResourceValue.Type.REFERENCE)

                    BinaryResourceValue::class.java
                        .getDeclaredField("data")
                        .apply { isAccessible = true }
                        .set(drawableAttr.typedValue(), newColorRscId.resourceId())

                    ZipWriter(apk, /* append = */ true).use {
                        it.deleteEntry(squareIconFile)
                        it.writeEntry(squareIconFile, iconXml.toByteArray(true))
                    }
                }

                patchIconFile(squareIconFile)
                patchIconFile(roundIconFile)
            }

            is IconReplacement.CustomImage -> {}
        }

        val newArscBytes = arsc.toByteArray(/* shrink = */ true)

        ZipWriter(apk, /* append = */ true).use {
            // val squareIcon = readAsset("icons/ic_logo_square.png")
            // val roundIcon = readAsset("icons/ic_logo_round.png")
            // val replacements = mapOf(
            //     arrayOf("_h_.png", "9MB.png", "Dy7.png", "kC0.png", "oEH.png", "RG0.png", "ud_.png", "W_3.png") to squareIcon
            // )
            //
            // for ((files, replacement) in replacements) {
            //     for (file in files) {
            //         val path = "res/$file"
            //         it.deleteEntry(path)
            //         it.writeEntry(path, replacement)
            //     }
            // }

            it.deleteEntry("resources.arsc")
            it.writeEntry("resources.arsc", newArscBytes)
        }
    }

    /**
     * From an APK, read the manifest's `icon` and `roundIcon` references to a resource.
     * This is then used to get the filename of the resource from `resources.arsc`.
     */
    private fun readManifestIconInfo(apk: File): ManifestIconInfo {
        val manifestBytes = ZipReader(apk).use { it.openEntry("AndroidManifest.xml")!!.read() }
        val manifest = BinaryResourceFile(manifestBytes)
        val mainChunk = manifest.chunks.single() as XmlChunk

        // Prefetch string indexes to avoid parsing the entire string pool
        val iconStringIdx = mainChunk.stringPool.indexOf("icon")
        val roundIconStringIdx = mainChunk.stringPool.indexOf("roundIcon")
        val applicationStringIdx = mainChunk.stringPool.indexOf("application")

        val applicationChunk = mainChunk.chunks.values.asSequence()
            .filterIsInstance<XmlStartElementChunk>()
            .find { it.nameIndex == applicationStringIdx }
            ?: error("Unable to find <application> in manifest")

        val squareIcon = applicationChunk.attributes
            .find { it.nameIndex() == iconStringIdx }
            ?: error("Unable to find android:icon in manifest")

        val roundIcon = applicationChunk.attributes
            .find { it.nameIndex() == roundIconStringIdx }
            ?: error("Unable to find android:roundIcon in manifest")

        assert(squareIcon.typedValue().type() == BinaryResourceValue.Type.REFERENCE)
        assert(roundIcon.typedValue().type() == BinaryResourceValue.Type.REFERENCE)

        return ManifestIconInfo(
            // Resource IDs into resources.arsc
            BinaryResourceIdentifier.create(squareIcon.typedValue().data()),
            BinaryResourceIdentifier.create(squareIcon.typedValue().data()),
        )
    }

    private data class ManifestIconInfo(
        val squareIcon: BinaryResourceIdentifier,
        val roundIcon: BinaryResourceIdentifier,
    )

    private fun readAsset(fileName: String): ByteArray =
        context.assets.open(fileName).use(InputStream::readBytes)
}
