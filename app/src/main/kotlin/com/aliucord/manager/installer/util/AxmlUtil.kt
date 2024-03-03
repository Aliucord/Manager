package com.aliucord.manager.installer.util

import com.aliucord.manager.util.find
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.*
import java.io.File

object AxmlUtil {
    /**
     * Read and parse a specific axml resource inside an APK
     * @param apk The source apk
     * @param resourcePath The full path to the axml file inside the apk, which may be flattened.
     */
    fun readAxml(apk: File, resourcePath: String): BinaryResourceFile {
        val bytes = ZipReader(apk).use { zip ->
            val entry = zip.openEntry(resourcePath)
                ?: error("APK missing resource file at $resourcePath")

            entry.read()
        }

        return try {
            BinaryResourceFile(bytes)
        } catch (t: Throwable) {
            throw Error("Failed to parse axml at $resourcePath", t)
        }
    }

    /**
     * Get the only top-level chunk in an axml file.
     */
    fun BinaryResourceFile.getMainAxmlChunk(): XmlChunk {
        return this.chunks.singleOrNull() as? XmlChunk
            ?: error("Invalid top-level axml chunk")
    }

    /**
     * Finds the first chunk with a matching [name] in a flattened chunk list.
     * @receiver The top level XmlChunk ([getMainAxmlChunk])
     */
    private fun XmlChunk.getStartElementChunk(name: String): XmlStartElementChunk? {
        val nameIdx = this.stringPool.indexOf(name)

        return this.chunks
            .find { it is XmlStartElementChunk && it.nameIndex == nameIdx }
            as? XmlStartElementChunk
    }

    /**
     * Finds the first attribute with a matching name (ignoring namespace)
     * in a starting element chunk.
     */
    private fun XmlStartElementChunk.getAttribute(name: String): XmlAttribute {
        val nameIdx = (this.parent as XmlChunk).stringPool.indexOf(name)

        return this.attributes
            .find { it.nameIndex() == nameIdx }
            ?: error("Failed to find $name attribute in an axml chunk")
    }

    fun patchAdaptiveIcon(
        apk: File,
        resourcePath: String,
        backgroundColor: BinaryResourceIdentifier? = null,
        foregroundIcon: BinaryResourceIdentifier? = null,
        monochromeIcon: BinaryResourceIdentifier? = null,
    ) {
        val xml = readAxml(apk, resourcePath)
        val xmlChunk = xml.getMainAxmlChunk()

        // Patch the background color resource reference
        if (backgroundColor != null) {
            val chunk = xmlChunk.getStartElementChunk("background")!!
            val attribute = chunk.getAttribute("drawable")
            attribute.typedValue().setValue(
                /* type = */ BinaryResourceValue.Type.REFERENCE,
                /* data = */ backgroundColor.resourceId(),
            )
        }

        // Patch the foreground drawable reference
        if (foregroundIcon != null) {
            val chunk = xmlChunk.getStartElementChunk("foreground")!!
            val attribute = chunk.getAttribute("drawable")
            attribute.typedValue().setValue(
                /* type = */ BinaryResourceValue.Type.REFERENCE,
                /* data = */ foregroundIcon.resourceId(),
            )
        }

        // Add or replace the monochrome drawable reference
        if (monochromeIcon != null) {
            // <monochrome> already exists, patch existing chunk
            val existingChunk = xmlChunk.getStartElementChunk("monochrome")
            if (existingChunk != null) {
                val attribute = existingChunk.getAttribute("drawable")
                attribute.typedValue().setValue(
                    /* type = */ BinaryResourceValue.Type.REFERENCE,
                    /* data = */ monochromeIcon.resourceId(),
                )
            }
            // Add a new start & end chunk since they don't exist
            // `<monochrome android:drawable="@drawable/xyz"></monochrome>
            else {
                val iconEndChunkIdx = xmlChunk.chunks
                    .indexOfLast { it is XmlEndElementChunk && it.name == "adaptive-icon" }

                val namespaceIdx = xmlChunk.stringPool.indexOf("http://schemas.android.com/apk/res/android")
                val drawableIdx = xmlChunk.stringPool.indexOf("drawable")
                val monochromeIdx = xmlChunk.stringPool.addString("monochrome")

                val startChunk = XmlStartElementChunk(
                    /* namespaceIndex = */ namespaceIdx,
                    /* nameIndex = */ monochromeIdx,
                    /* idIndex = */ -1,
                    /* classIndex = */ -1,
                    /* styleIndex = */ -1,
                    /* attributes = */
                    listOf(
                        XmlAttribute(
                            /* namespaceIndex = */ -1,
                            /* nameIndex = */ drawableIdx,
                            /* rawValueIndex = */ -1,
                            /* typedValue = */
                            BinaryResourceValue(
                                /* size = */ BinaryResourceValue.SIZE,
                                /* type = */ BinaryResourceValue.Type.REFERENCE,
                                /* data = */ monochromeIcon.resourceId(),
                            ),
                            // This is wrong but it doesn't matter here as long as this attribute isn't stringified
                            /* parent = */ null,
                        )
                    ),
                    /* parent = */ xmlChunk,
                )
                val endChunk = XmlEndElementChunk(
                    /* namespaceIndex = */ namespaceIdx,
                    /* nameIndex = */ monochromeIdx,
                    /* parent = */ xmlChunk,
                )

                xmlChunk.addChunk(iconEndChunkIdx, startChunk)
                xmlChunk.addChunk(iconEndChunkIdx + 1, endChunk)
            }
        }

        ZipWriter(apk, /* append = */ true).use { zip ->
            zip.deleteEntry(resourcePath)
            zip.writeEntry(resourcePath, xml.toByteArray(/* shrink = */ true))
        }
    }
}
