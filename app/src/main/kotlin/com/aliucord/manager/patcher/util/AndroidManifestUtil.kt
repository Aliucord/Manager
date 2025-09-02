package com.aliucord.manager.patcher.util

import com.aliucord.manager.util.find
import com.google.devrel.gmscore.tools.apk.arsc.*

class AndroidManifestUtil(manifestBytes: ByteArray) {
    private val axml = BinaryResourceFile(manifestBytes)
    private val rootChunk = this.axml.chunks.first() as XmlChunk
    private val resourceMapChunk = rootChunk.chunks.find { it is XmlResourceMapChunk }!! as XmlResourceMapChunk

    /**
     * A string index of the Android namespace used for attributes.
     */
    val androidNSIdx by lazy { rootChunk.stringPool.addString(ANDROID_NAMESPACE, true) }

    /**
     * Serializes this AndroidManifest back into AXML bytes.
     */
    fun toByteArray(): ByteArray = axml.toByteArray()

    /**
     * An attribute to be added onto the `<application>` manifest element.
     * @param name The name of the attribute (excluding namespace)
     * @param resourceId The Android resource id of the attribute, as defined in [android.R.attr].
     *   For example, [android.R.attr.useEmbeddedDex]
     * @param value The value of the attribute. Refer to [makeAttribute] for what types are currently supported.
     */
    data class ApplicationAttribute(
        val name: String,
        val resourceId: Int,
        val value: Any,
    )

    /**
     * Sets attributes on the `<application>` manifest element.
     * If an attribute already exists, its value is overridden.
     */
    fun addApplicationAttributes(vararg attributes: ApplicationAttribute) {
        val applicationChunk = rootChunk.chunks
            .find { it is XmlStartElementChunk && it.name == "application" }
            ?.let { it as XmlStartElementChunk }
            ?: throw IllegalStateException("Unable to find <application> XML chunk")

        val attributesData = attributes.map {
            XmlAttributeData(
                namespaceIdx = androidNSIdx,
                nameIdx = rootChunk.stringPool.addString(it.name, true),
                resourceId = it.resourceId,
                value = it.value,
            )
        }

        applicationChunk.mergeChunkAttributes(attributesData)
    }

    /**
     * Adds a `<uses-permission>` declaration to the manifest, placed after all the other permissions.
     * This handles adding duplicate permissions.
     */
    fun addPermission(newPermission: String) {
        var lastPermissionChunkEndIdx = 0

        // Find the index of the last <uses-permission>
        for (i in 0..<rootChunk.chunks.size) {
            when (val chunk = rootChunk.chunks[i]) {
                is XmlStartElementChunk if chunk.name == "uses-permission" -> {
                    val permissionAttr = chunk.attributes.find { it.name() == "name" }!!
                    val permission = permissionAttr.rawValue()

                    // Permission already added
                    if (permission == newPermission)
                        return
                }

                is XmlEndElementChunk if chunk.name == "uses-permission" -> {
                    lastPermissionChunkEndIdx = i
                }
            }
        }

        // Add all necessary strings to global string pool
        val nameStringIdx = rootChunk.stringPool.addString("name", true)
        val usesPermissionStringIdx = rootChunk.stringPool.addString("uses-permission", true)

        // Create a new <uses-permission android:name="..."> start chunk
        val startChunkAttrs = mutableListOf<XmlAttribute>()
        val startChunk = XmlStartElementChunk(
            /* namespaceIndex = */ -1,
            /* nameIndex = */ usesPermissionStringIdx,
            /* idIndex = */ -1,
            /* classIndex = */ -1,
            /* styleIndex = */ -1,
            /* attributes = */ startChunkAttrs,
            /* parent = */ rootChunk,
        )
        startChunkAttrs += makeAttribute(
            parent = startChunk,
            namespaceIdx = androidNSIdx,
            nameIdx = nameStringIdx,
            resourceId = android.R.attr.name,
            value = newPermission,
        )

        // Create a </uses-permission> chunk
        val endChunk = XmlEndElementChunk(
            /* namespaceIndex = */ -1,
            /* nameIndex = */ usesPermissionStringIdx,
            /* parent = */ rootChunk,
        )

        rootChunk.addChunk(lastPermissionChunkEndIdx + 1, startChunk)
        rootChunk.addChunk(lastPermissionChunkEndIdx + 2, endChunk)
    }

    /**
     * Gets all elements accessible at a certain path into the document.
     *
     * For example, passing in `manifest.application.receiver` for [path] will retrieve **all**
     * `<receiver>` elements that are under a `<application>` and then `<manifest>`. Note that this means
     * that the parents can all be different, as these nodes are flattened. This method does not differentiate
     * between different parents when selecting child nodes.
     *
     * Note that namespaces are ignored here.
     *
     * @return Indexes into the chunks array of the [rootChunk]
     */
    private fun indexOf(path: String): IntArray {
        val parts = path.split('.')

        TODO()
    }

    /**
     * Adds or overrides attributes on this start chunk.
     */
    private fun XmlStartElementChunk.mergeChunkAttributes(newAttributes: List<XmlAttributeData>) {
        val remainingAttributes = newAttributes.associateBy { it.nameIdx } as MutableMap<Int, XmlAttributeData>

        for (i in 0..<this.attributes.size) {
            val attribute = this.attributes[i]
            val newAttributeData = remainingAttributes[attribute.nameIndex()] ?: continue

            this.attributes[i] = makeAttribute(
                parent = this,
                namespaceIdx = newAttributeData.namespaceIdx,
                nameIdx = attribute.nameIndex(),
                resourceId = newAttributeData.resourceId, // FIXME: add this to resource chunk if doesn't exist
                value = newAttributeData.value,
            )

            remainingAttributes -= attribute.nameIndex()
        }

        this.attributes += remainingAttributes.map { (_, data) ->
            makeAttribute(
                parent = this,
                namespaceIdx = data.namespaceIdx,
                nameIdx = rootChunk.stringPool.addString(name, true),
                resourceId = data.resourceId, // FIXME: add this to resource chunk if doesn't exist
                value = data.value,
            )
        }
    }

    /**
     * Creates a [XmlAttribute] for a [XmlStartElementChunk].
     *
     * @param parent The node this attribute is located on.
     * @param namespaceIdx The string pool index of the namespace this attribute is in.
     * @param nameIdx The string pool index of the attribute name.
     * @param resourceId The resource this attribute corresponds to. (ie. [android.R.attr.name] or [android.R.attr.useEmbeddedDex])
     * @param value The raw value. Supported value types:
     * - `null`
     * - [String]
     * - [Boolean]
     * @return A new attribute to be added to the parent chunk's attributes.
     */
    private fun makeAttribute(parent: XmlNodeChunk, namespaceIdx: Int?, nameIdx: Int, resourceId: Int?, value: Any?): XmlAttribute {
        val typedValue = BinaryResourceValue(
            /* type = */ when (value) {
                null -> BinaryResourceValue.Type.NULL
                is String -> BinaryResourceValue.Type.STRING
                is Boolean -> BinaryResourceValue.Type.INT_BOOLEAN
                else -> throw UnsupportedOperationException("Handling ${value::class.simpleName} attribute types is unsupported!")
            },
            /* data = */ when (value) {
                null -> 0
                is String -> rootChunk.stringPool.addString(value, true)
                is Boolean -> if (value) 1 else 0
                else -> error("unreachable")
            }
        )
        val rawValueIdx = when (value) {
            is String -> typedValue.data()
            else -> -1
        }

        // FIXME
        // if (resourceId != null)
        //     resourceMapChunk.addResourceId(resourceId)

        return XmlAttribute(
            /* namespaceIndex = */ namespaceIdx ?: -1,
            /* nameIndex = */ nameIdx,
            /* rawValueIndex = */ rawValueIdx,
            /* typedValue = */ typedValue,
            /* parent = */ parent,
        )
    }

    private data class XmlAttributeData(
        val namespaceIdx: Int,
        val nameIdx: Int,
        val resourceId: Int?,
        val value: Any?,
    )

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
