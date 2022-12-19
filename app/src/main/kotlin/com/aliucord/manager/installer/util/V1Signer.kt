package com.aliucord.manager.installer.util

import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import io.ktor.util.*
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.*
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.jar.Attributes
import java.util.jar.Manifest

/**
 * APK/Jar/Zip V1 Signature signer
 *
 * Based on:
 * - https://gist.github.com/mmuszkow/10288441
 * - https://github.com/fornwall/apksigner/blob/fde57568e25f7ef0aa148f6bc120a7f4131443dd/src/main/java/net/fornwall/apksigner/ZipSigner.java
 * - https://github.com/Aliucord/Aliucord/blob/5d5a2844da256c532c7cca6b65d6be374c3a80d8/installer/android/app/src/main/java/com/aliucord/installer/Signer.java
 */
object V1Signer {
    private val v1StripPattern = "^META-INF/(.*)[.](MF|SF|RSA|DSA)$".toRegex()

    fun signApkWithV1(apkFile: File, keySet: Signer.KeySet) {
        val fileDigests = LinkedHashMap<String, String>()
        val sectionDigests = LinkedHashMap<String, String>()
        val toRemove = LinkedList<String>()

        ZipReader(apkFile).use { zip ->
            zip.entries.forEach {
                if (v1StripPattern.matches(it.name)) {
                    toRemove.add(it.name)
                } else {
                    fileDigests[it.name] = it.read().sha1DigestBase64()
                }
            }
        }

        val digestAttr = Attributes.Name("SHA1-Digest")
        val createdByAttr = Attributes.Name("SHA1-Digest")

        val mainManifest = run {
            val manifest = Manifest().apply {
                mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
                mainAttributes[createdByAttr] = "Aliucord Manager"
            }

            for ((name, digest) in fileDigests) {
                val attributes = Attributes()
                attributes[digestAttr] = digest
                manifest.entries[name] = attributes
                sectionDigests[name] = hashEntrySection(name, attributes)
            }

            manifest
        }

        val certManifest = Manifest().apply {
            mainAttributes[createdByAttr] = "Aliucord Manager"
            mainAttributes[Attributes.Name.SIGNATURE_VERSION] = "1.0"
            mainAttributes[Attributes.Name("SHA1-Digest-Manifest")] = getManifestHash(mainManifest)
            mainAttributes[Attributes.Name("SHA1-Digest-Manifest-Main-Attributes")] = Manifest().let {
                it.mainAttributes.putAll(mainManifest.mainAttributes)
                getManifestHash(it)
            }
        }

        for ((name, digest) in sectionDigests) {
            certManifest.entries[name] = Attributes()
                .also { it[digestAttr] = digest }
        }

        ZipWriter(apkFile, true).use { zip ->
            toRemove.forEach(zip::deleteEntry)

            ByteArrayOutputStream()
                .use { mainManifest.write(it); it.toByteArray() }
                .let { zip.writeEntry("META-INF/MANIFEST.MF", it) }

            val certBytes = ByteArrayOutputStream()
                .use { certManifest.write(it); it.toByteArray() }
                .also { zip.writeEntry("META-INF/CERT.SF", it) }

            zip.writeEntry("META-INF/CERT.RSA", signSigFile(keySet, certBytes))
        }
    }

    private fun hashEntrySection(name: String, attrs: Attributes): String {
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"

        val emptyLen = ByteArrayOutputStream().use {
            manifest.write(it)
            it.size()
        }

        manifest.entries[name] = attrs

        return ByteArrayOutputStream()
            .use { manifest.write(it); it.toByteArray() }
            .let { Arrays.copyOfRange(it, emptyLen, it.size) }
            .sha1DigestBase64()
    }

    private fun getManifestHash(manifest: Manifest): String {
        return ByteArrayOutputStream()
            .use { manifest.write(it); it.toByteArray() }
            .sha1DigestBase64()
    }

    private fun signSigFile(keySet: Signer.KeySet, content: ByteArray): ByteArray {
        val msg: CMSTypedData = CMSProcessableByteArray(content)
        val certs = JcaCertStore(listOf<Any>(keySet.publicKey))

        val sha1Signer = JcaContentSignerBuilder("SHA1withRSA")
            .build(keySet.privateKey)
        val digestCalculatorProvider = JcaDigestCalculatorProviderBuilder()
            .build()
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(digestCalculatorProvider)
            .setDirectSignature(true)
            .build(sha1Signer, keySet.publicKey)

        val gen = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(signerInfoGenerator)
            addCertificates(certs)
        }

        return gen.generate(msg, false)
            .toASN1Structure()
            .getEncoded("DER")
    }

    private fun ByteArray.sha1DigestBase64() = MessageDigest
        .getInstance("SHA1")
        .digest(this)
        .encodeBase64()
}
