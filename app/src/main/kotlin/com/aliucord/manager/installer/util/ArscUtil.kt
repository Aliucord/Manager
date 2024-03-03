package com.aliucord.manager.installer.util

import androidx.collection.MutableObjectList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.diamondminer88.zip.ZipReader
import com.google.devrel.gmscore.tools.apk.arsc.*
import java.io.File

object ArscUtil {
    /**
     * Read and parse `resources.arsc` from an APK.
     */
    fun readArsc(apk: File): BinaryResourceFile {
        val bytes = ZipReader(apk).use { zip ->
            val entry = zip.openEntry("resources.arsc")
                ?: error("APK missing resources.arsc")

            entry.read()
        }

        return try {
            BinaryResourceFile(bytes)
        } catch (t: Throwable) {
            throw Error("Failed to parse resources.arsc", t)
        }
    }

    /**
     * Get the only top-level chunk in an arsc file.
     */
    fun BinaryResourceFile.getMainArscChunk(): ResourceTableChunk {
        return this.chunks.singleOrNull() as? ResourceTableChunk
            ?: error("Invalid top-level resources.arsc chunk")
    }

    /**
     * Get a singular package chunk in an arsc file.
     */
    fun BinaryResourceFile.getPackageChunk(): PackageChunk {
        return this.getMainArscChunk().packages.singleOrNull()
            ?: error("resources.arsc must contain exactly 1 package chunk")
    }

    /**
     * Adds a new color resource to all configuration variants in an arsc package.
     *
     * @param name The new resource name.
     * @param color The value of the new color resource.
     * @return The resource ID of the newly added resource.
     */
    fun PackageChunk.addColorResource(
        name: String,
        color: Color,
    ): BinaryResourceIdentifier {
        // Add a new resource entry to the color "type spec chunk" and,
        // a new resource entry to all matching "type chunks"

        val colorSpecChunk = this.getTypeSpecChunk("color")
        val colorTypeChunks = this.getTypeChunks("color")

        // Add a new string to the pool to be used as a key
        val stringKeyIdx = this.keyStringPool.addString(name)

        // Add a new resource entry to the type spec chunk
        val resourceIdx = colorSpecChunk.addResource(/* flags = */ 0)

        for (typeChunk in colorTypeChunks) {
            val entry = TypeChunk.Entry(
                /* headerSize = */ 8,
                /* flags = */ 0,
                /* keyIndex = */ stringKeyIdx,
                /* value = */
                BinaryResourceValue(
                    /* size = */ BinaryResourceValue.SIZE,
                    /* type = */ BinaryResourceValue.Type.INT_COLOR_ARGB8,
                    /* data = */ color.toArgb(),
                ),
                /* values = */ null, // not a complex resource
                /* parentEntry = */ 0, // not a complex resource
                /* parent = */ typeChunk,
            )

            // TODO: add a "addEntry(TypeChunk.Entry)" method to lib
            (typeChunk.entries as MutableObjectList).add(entry)
        }

        return BinaryResourceIdentifier.create(
            /* packageId = */ this.id,
            /* typeId = */ colorSpecChunk.id,
            /* entryId = */ resourceIdx,
        )
    }

    /**
     * In an arsc file, for a specific resource in a configuration, get it's value.
     *
     * @param resourceId The target resource id.
     * @param configurationName The target configuration variant of the resource. (ex: `anydpi-v26`, `xxhdpi`, `ldtrl-mpi`, etc.)
     * @return The string value of the resource, which should be a file path inside the apk.
     */
    fun ResourceTableChunk.getResourceFileName(
        resourceId: BinaryResourceIdentifier,
        configurationName: String,
    ): String {
        val packageChunk = this.packages.find { it.id == resourceId.packageId() }
            ?: error("Unable to find target resource")

        val typeChunk = packageChunk.getTypeChunks(resourceId.typeId())
            .find { it.configuration.toString() == configurationName }
            ?: error("Unable to find target resource")

        val entry = try {
            typeChunk.entries[resourceId.entryId()]
        } catch (_: Throwable) {
            error("Unable to find target resource")
        }

        if (entry.isComplex || entry.value().type() != BinaryResourceValue.Type.STRING)
            error("Target resource value type is not STRING")

        val valueIdx = entry.value().data()
        val value = this.stringPool.getString(valueIdx)

        return value
    }

    // fun getResourceFileName(
    //     globalStringPool: StringPoolChunk,
    //     arscPackage: PackageChunk,
    //     typeName: String,
    //     resourceName: String,
    //     configurationName: String,
    // ): String {
    //     val resourceNameIdx = arscPackage.keyStringPool.indexOf(resourceName)
    //
    //     arscPackage.getTypeChunks(typeName).forEach { chunk ->
    //         if (chunk.configuration.toString() != configurationName)
    //             return@forEach
    //
    //         chunk.entries.forEach forEach2@{ entry ->
    //             if (entry?.keyIndex() != resourceNameIdx
    //                 || entry.isComplex
    //                 || entry.value().type() != BinaryResourceValue.Type.STRING
    //             ) {
    //                 return@forEach2
    //             }
    //
    //             val valueIdx = entry.value().data()
    //             val value = globalStringPool.getString(valueIdx)
    //
    //             return value
    //         }
    //     }
    //
    //     throw IllegalArgumentException("Unable to find the target resource in arsc")
    // }
}
