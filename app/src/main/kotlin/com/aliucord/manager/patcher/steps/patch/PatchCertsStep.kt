package com.aliucord.manager.patcher.steps.patch

import android.app.Application
import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.util.ArscUtil
import com.aliucord.manager.patcher.util.ArscUtil.addResource
import com.aliucord.manager.patcher.util.ArscUtil.getMainArscChunk
import com.aliucord.manager.patcher.util.ArscUtil.getPackageChunk
import com.aliucord.manager.patcher.util.ArscUtil.getResourceFileNames
import com.aliucord.manager.patcher.util.AxmlUtil.getMainAxmlChunk
import com.aliucord.manager.util.find
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Adds a network security config that manually adds new CA root certificates.
 * This is useful for old Android devices that do not have updated root certs.
 */
class PatchCertsStep : Step(), KoinComponent {
    private val context: Application by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_certs

    // Manager's (this application) network security config is used as a template
    // to inject into Aliucord, except with the resource ids pointing to certificate files changed
    // to new ones injected into the patched app's arsc
    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        if (Build.VERSION.SDK_INT >= 26) {
            container.log("Modern device detected, skipping injecting root certs")
            state = StepState.Skipped
            return
        }

        container.log("Parsing resources.arsc")
        val arsc = ArscUtil.readArsc(apk)
        val resourcesChunk = arsc.getMainArscChunk()
        val packageChunk = arsc.getPackageChunk()

        container.log("Creating new raw resources in arsc")
        val certificateIds = CERTIFICATES.keys.map { certificateName ->
            packageChunk.addResource(
                typeName = "raw",
                resourceName = certificateName,
                configurations = { it.isDefault },
                valueType = BinaryResourceValue.Type.STRING,
                valueData = resourcesChunk.stringPool.addString("res/$certificateName.der"),
            )
        }

        container.log("Generating new network security config AXML")
        val newNetworkSecurityConfigBytes = generateNetworkConfig(certificateIds)

        container.log("Parsing existing AndroidManifest.xml")
        val networkSecurityConfigId = getNetworkSecurityConfigResourceId(apk)
        val networkSecurityConfigPath = resourcesChunk.getResourceFileNames(
            resourceId = networkSecurityConfigId,
            configurations = { it.isDefault },
        ).single()

        ZipWriter(apk, /* append = */ true).use { zip ->
            zip.deleteEntries(networkSecurityConfigPath, "resources.arsc")

            container.log("Writing new network security config AXML")
            zip.writeEntry(networkSecurityConfigPath, newNetworkSecurityConfigBytes)

            container.log("Writing new arsc")
            zip.writeEntry("resources.arsc", arsc.toByteArray())

            for ((name, id) in CERTIFICATES) {
                container.log("Writing $name CA certificate to apk")

                val bytes = context.resources.openRawResource(id).use { it.readBytes() }
                zip.writeEntry("res/$name.der", bytes)
            }
        }
    }

    /**
     * From an APK, read the manifest's `android:networkSecurityConfig` references to a resource.
     * This is then used to get the filename of the resource from `resources.arsc`.
     */
    fun getNetworkSecurityConfigResourceId(apk: File): BinaryResourceIdentifier {
        val manifestBytes = ZipReader(apk).use {
            it.openEntry("AndroidManifest.xml")?.read()
        } ?: error("APK missing manifest")
        val manifest = BinaryResourceFile(manifestBytes)
        val mainChunk = manifest.getMainAxmlChunk()

        // Prefetch string indexes to avoid parsing the entire string pool
        val networkSecurityConfigStringIdx = mainChunk.stringPool.indexOf("networkSecurityConfig")
        val applicationStringIdx = mainChunk.stringPool.indexOf("application")

        val applicationChunk = mainChunk.chunks
            .find { it is XmlStartElementChunk && it.nameIndex == applicationStringIdx } as? XmlStartElementChunk
            ?: error("Unable to find <application> in manifest")
        val networkSecurityConfig = applicationChunk.attributes
            .find { it.nameIndex() == networkSecurityConfigStringIdx }
            ?: error("Unable to find android:networkSecurityConfig in manifest")

        assert(networkSecurityConfig.typedValue().type() == BinaryResourceValue.Type.REFERENCE)

        return BinaryResourceIdentifier.create(networkSecurityConfig.typedValue().data())
    }

    /**
     * This generates a binary AXML representation a network security config similar to the one
     * manager uses [R.xml.network_security_config], except with resource IDs generated for the patched APK.
     */
    private fun generateNetworkConfig(certificateIds: List<BinaryResourceIdentifier>): ByteArray {
        val axml = BinaryResourceFile(byteArrayOf())
        val xmlChunk = XmlChunk(null)
        val strings = StringPoolChunk(xmlChunk)

        axml.appendChunk(xmlChunk)
        xmlChunk.appendChunk(strings)
        xmlChunk.appendChunk(XmlResourceMapChunk(intArrayOf(), xmlChunk))

        // Nested chunks
        // @formatter:off
        val chunkNames = arrayOf("network-security-config", "base-config", "trust-anchors")
        for (chunkName in chunkNames) {
            xmlChunk.appendChunk(XmlStartElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString(chunkName),
                /* idIndex = */ -1,
                /* classIndex = */ -1,
                /* styleIndex = */ -1,
                /* attributes = */ emptyList(),
                /* parent = */ xmlChunk,
            ))
        }

        // Allow "system" certificates
        run {
            val certificateChunk = XmlStartElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString("certificates", /* deduplicate = */ true),
                /* idIndex = */ -1,
                /* classIndex = */ -1,
                /* styleIndex = */ -1,
                /* attributes = */ listOf(),
                /* parent = */ xmlChunk,
            )
            certificateChunk.attributes += XmlAttribute(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ xmlChunk.stringPool.addString("src", /* deduplicate = */ true),
                /* rawValueIndex = */ xmlChunk.stringPool.addString("system"),
                /* typedValue = */ BinaryResourceValue(
                    /* type = */ BinaryResourceValue.Type.STRING,
                    /* data = */ xmlChunk.stringPool.addString("system", /* deduplicate = */ true),
                ),
                /* parent = */ certificateChunk,
            )

            xmlChunk.appendChunk(certificateChunk)
            xmlChunk.appendChunk(XmlEndElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString("certificates", /* deduplicate = */ true),
                /* parent = */ xmlChunk,
            ))
        }

        // Add custom certificate references
        for (certificateId in certificateIds) {
            val certificateChunk = XmlStartElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString("certificates", /* deduplicate = */ true),
                /* idIndex = */ -1,
                /* classIndex = */ -1,
                /* styleIndex = */ -1,
                /* attributes = */ listOf(),
                /* parent = */ xmlChunk,
            )
            certificateChunk.attributes += XmlAttribute(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ xmlChunk.stringPool.addString("src", /* deduplicate = */ true),
                /* rawValueIndex = */ -1,
                /* typedValue = */ BinaryResourceValue(
                    /* type = */ BinaryResourceValue.Type.REFERENCE,
                    /* data = */ certificateId.resourceId(),
                ),
                /* parent = */ certificateChunk,
            )

            xmlChunk.appendChunk(certificateChunk)
            xmlChunk.appendChunk(XmlEndElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString("certificates", /* deduplicate = */ true),
                /* parent = */ xmlChunk,
            ))
        }

        // Reverse nested chunks
        for (chunkName in chunkNames.reversed()) {
            xmlChunk.appendChunk(XmlEndElementChunk(
                /* namespaceIndex = */ -1,
                /* nameIndex = */ strings.addString(chunkName, /* deduplicate = */ true),
                /* parent = */ xmlChunk,
            ))
        }
        // @formatter:on

        return axml.toByteArray()
    }

    private companion object {
        val CERTIFICATES = mapOf(
            "globalsign_root_r4" to R.raw.globalsign_root_r4,
            "gts_root_r1" to R.raw.gts_root_r1,
            "gts_root_r2" to R.raw.gts_root_r2,
            "gts_root_r3" to R.raw.gts_root_r3,
            "gts_root_r4" to R.raw.gts_root_r4,
            "isrg_root_x1" to R.raw.isrg_root_x1,
            "isrg_root_x2" to R.raw.isrg_root_x2,
        )
    }
}
