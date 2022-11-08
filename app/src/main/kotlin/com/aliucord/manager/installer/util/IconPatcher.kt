package com.aliucord.manager.installer.util

import android.app.Application
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import pxb.android.arsc.*
import pxb.android.axml.*
import java.io.File

class IconPatcher(
    private val application: Application
) {
    fun patchIcons(apkFile: File) {
        val (squareIconPaths, roundIconPaths, arscPkg) = ZipReader(apkFile).use { zip ->
            val manifestReader = zip.openEntry("AndroidManifest.xml")?.read()
                ?.let { AxmlReader(it) }
                ?: throw Error("No manifest in apk")

            val (squareIconId, roundIconId) = getIconRscIds(manifestReader)

            val pkg = zip.openEntry("resources.arsc")?.read()
                .let { ArscParser(it).parse() }
                .takeIf { it.size == 1 }
                ?.firstOrNull()
                ?: throw Error("Invalid number of packages in arsc")

            // ArscDumper.dump(listOf(pkg))

            Triple(
                pkg.findMipmapPathsAndXml(squareIconId),
                pkg.findMipmapPathsAndXml(roundIconId),
                pkg
            )
        }

        val (squareIconXml, roundIconXml) = ZipReader(apkFile).use { zip ->
            val square = squareIconPaths?.let { (_, xmlPath) ->
                zip.openEntry(xmlPath)?.read()
                    ?: throw Error("No square icon $xmlPath in apk")
            }

            val round = roundIconPaths?.let { (_, xmlPath) ->
                zip.openEntry(xmlPath)?.read()
                    ?: throw Error("No round icon $xmlPath in apk")
            }

            square to round
        }

        // val colorsType = arscPkg.types.values.find { it.name == "color" }!!
        // val aliucordColorValue = Value(0x1d, -0xf_00_c8_55, null)
        // val aliucordColorSpecId = colorsType.specs.last().id + 1
        // val aliucordColorResId = (arscPkg.id shl 24) or (colorsType.id shl 16) or aliucordColorSpecId
        // colorsType.specs = Arrays.copyOf(colorsType.specs, colorsType.specs.size + 1)
        // colorsType.specs[aliucordColorSpecId] = ResSpec(aliucordColorSpecId).apply {
        //     flags = 0
        //     name = "aliucord"
        // }
        // val aliucordColorSpec = colorsType.specs[aliucordColorSpecId]
        // colorsType.configs.first().resources[aliucordColorSpecId] = ResEntry(0x00, aliucordColorSpec).apply { value = aliucordColorValue }
        // ArscDumper.dump(listOf(arscPkg))

        ZipWriter(apkFile, true).use { zip ->
            zip.deleteEntry("resources.arsc")
            zip.writeEntry("resources.arsc", ArscWriter(listOf(arscPkg)).toByteArray())

            val squareIcon = application.assets.open("icons/ic_logo_square.png").readBytes()
            val roundIcon = application.assets.open("icons/ic_logo_round.png").readBytes()

            squareIconPaths?.first?.forEach {
                zip.deleteEntry(it)
                zip.writeEntry(it, squareIcon)
            }
            roundIconPaths?.first?.forEach {
                zip.deleteEntry(it)
                zip.writeEntry(it, roundIcon)
            }
        }

        // val adaptiveIcon = zip.getContent("res/mipmap-anydpi-v26/ic_logo_square_beta.xml").array()
        //     .let { AxmlReader(it) }
        // val writer = AxmlWriter()
        //
        // var foregroundIconId: Int? = null
        // var monochromeIconId: Int? = null
        //
        // adaptiveIcon.accept(
        //     object : AxmlVisitor(writer) {
        //         override fun child(ns: String?, name: String?): NodeVisitor {
        //             return object : NodeVisitor(super.child(ns, name)) {
        //                 override fun child(ns: String?, parentName: String?): NodeVisitor {
        //                     return object : NodeVisitor(super.child(ns, parentName)) {
        //                         var monochromePresent = false
        //
        //                         override fun attr(ns: String?, name: String?, resourceId: Int, type: Int, obj: Any?) {
        //                             return when (parentName) {
        //                                 "foreground" -> {
        //                                     foregroundIconId = obj as Int
        //                                     super.attr(ns, name, resourceId, type, obj)
        //                                 }
        //                                 "background" -> {
        //                                     // TODO: try to change to use focus_primary_500
        //                                     super.attr(
        //                                         ns,
        //                                         name,
        //                                         resourceId,
        //                                         TYPE_REFERENCE,
        //                                         0x7f06010a
        //                                     )
        //                                 }
        //                                 "monochrome" -> {
        //                                     monochromePresent = true
        //                                     monochromeIconId = obj as Int
        //                                     super.attr(ns, name, resourceId, type, obj)
        //                                 }
        //                                 else -> super.attr(ns, name, resourceId, type, obj)
        //                             }
        //                         }
        //
        //                         override fun end() {
        //                             if (!monochromePresent) {
        //                                 // Add monochrome icon tag
        //                                 // TODO: add to arsc table
        //                                 super.attr(
        //                                     "http://schemas.android.com/apk/res/android",
        //                                     "drawable",
        //                                     android.R.attr.drawable,
        //                                     NodeVisitor.TYPE_REFERENCE,
        //                                     foregroundIconId
        //                                 )
        //                             }
        //
        //                             super.end()
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // )

        // zip.delete("res/mipmap-anydpi-v26/ic_logo_square_beta.xml")
        // zip.delete("res/mipmap-anydpi-v26/ic_logo_round_beta.xml")
        // val bytes = writer.toByteArray()
        // zip.add(BytesSource(bytes, "res/mipmap-anydpi-v26/ic_logo_square_beta.xml", Deflater.DEFAULT_COMPRESSION))
        // zip.add(BytesSource(bytes, "res/mipmap-anydpi-v26/ic_logo_round_beta.xml", Deflater.DEFAULT_COMPRESSION))
    }

    /**
     * Finds the adaptive xml icon path and other paths
     * @return Pair<other png paths, adaptive xml path>
     */
    private fun Pkg.findMipmapPathsAndXml(rscId: Int?): Pair<List<String>, String>? {
        return rscId
            ?.let { this.getResPaths("mipmap", it) }
            ?.partition { !it.endsWith(".xml") }
            ?.let { it.first to it.second.single() }
    }

    private fun Pkg.getResPaths(arscType: String, rscId: Int): List<String> {
        val type = this.types.values.find { it.name == arscType }
            ?: throw Error("No $arscType type in arsc")

        val resPrefix = (this.id shl 24) or (type.id shl 16)

        return type.configs.flatMap { config ->
            config.resources
                .filterValues { (it.spec.id or resPrefix) == rscId }
                .map { (_, res) -> (res.value as Value).raw }
        }
    }

    /**
     * Get the regular icon and round icon resource ids from AndroidManifest.xml
     * @return Pair<square icon id, round icon id>
     */
    private fun getIconRscIds(reader: AxmlReader): Pair<Int?, Int?> {
        var squareIconId: Int? = null
        var roundIconId: Int? = null

        reader.accept(
            object : AxmlVisitor(AxmlWriter()) {
                override fun child(ns: String?, name: String?): NodeVisitor {
                    return object : NodeVisitor(super.child(ns, name)) {
                        override fun child(ns: String?, name: String?): NodeVisitor {
                            val nv = super.child(ns, name)

                            return when (name) {
                                "application" -> object : NodeVisitor(nv) {
                                    override fun attr(ns: String?, name: String?, resourceId: Int, type: Int, value: Any?) {
                                        when (name) {
                                            "icon" -> squareIconId = value as Int
                                            "roundIcon" -> roundIconId = value as Int
                                        }
                                    }
                                }
                                else -> nv
                            }
                        }
                    }
                }
            }
        )

        return squareIconId to roundIconId
    }
}
