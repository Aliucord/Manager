/*
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */
package com.aliucord.manager

import com.aliucord.libzip.Zip
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object Signer {
    private val stripPattern = Regex("^META-INF/(.*)[.](MF|SF|RSA|DSA)$")

    fun newKeystore(out: File?) {
        val password = "password".toCharArray()
        val key = createKey()

        with(KeyStore.getInstance("BKS", "BC")) {
            load(null, password)
            setKeyEntry("alias", key.privateKey, password, arrayOf<Certificate>(key.publicKey))
            store(out?.outputStream(), password)
        }
    }

    @Throws(Exception::class)
    private fun createKey(): KeySet {
        val pair = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }
        var serialNumber: BigInteger

        do serialNumber = SecureRandom().nextInt().toBigInteger()
        while (serialNumber < BigInteger.ZERO)

        val x500Name = X500Name("CN=Aliucord Installer")
        val builder = X509v3CertificateBuilder(
            x500Name,
            serialNumber,
            Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L),
            Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L),
            Locale.ENGLISH,
            x500Name,
            SubjectPublicKeyInfo.getInstance(pair.public.encoded)
        )
        val signer = JcaContentSignerBuilder("SHA1withRSA").build(pair.private)

        return KeySet(JcaX509CertificateConverter().getCertificate(builder.build(signer)), pair.private)
    }

    // based on https://gist.github.com/mmuszkow/10288441
    // and https://github.com/fornwall/apksigner/blob/master/src/main/java/net/fornwall/apksigner/ZipSigner.java
    @Throws(Exception::class)
    fun signApk(apkFile: File) {
        val ks = File(apkFile.parent, "ks.keystore").also { keyStore ->
            if (!keyStore.exists()) newKeystore(keyStore)
        }

        val keyStore = KeyStore.getInstance("BKS", "BC")
        ks.inputStream().use { keyStore.load(it, null) }

        val alias = keyStore.aliases().nextElement()
        val password = "password".toCharArray()
        val keySet = KeySet(
            keyStore.getCertificate(alias) as X509Certificate,
            keyStore.getKey(alias, password) as PrivateKey
        )
        var zip = Zip(apkFile.absolutePath, 6, 'r')
        val dig = MessageDigest.getInstance("SHA1")
        val digests = LinkedHashMap<String, String>()
        val filesToRemove = ArrayList<String>()
        repeat(zip.totalEntries) { i ->
            zip.openEntryByIndex(i)
            val name = zip.entryName

            if (stripPattern.matches(name))
                filesToRemove.add(name)
            else
                digests[name] = dig.digest(zip.readEntry()).toBase64()

            zip.closeEntry()
        }
        zip.close()
        zip = Zip(apkFile.absolutePath, 6, 'a')

        filesToRemove.forEach(zip::deleteEntry)
        val sectionDigests = LinkedHashMap<String, String>()
        var manifest = Manifest()
        var attrs = manifest.mainAttributes
        attrs[Attributes.Name.MANIFEST_VERSION] = "1.0"
        attrs[Attributes.Name("Created-By")] = "Aliucord Installer"
        val digestAttr = Attributes.Name("SHA1-Digest")

        for ((name, value) in digests) {
            val attributes = Attributes()

            attributes[digestAttr] = value
            manifest.entries[name] = attributes
            sectionDigests[name] = hashEntrySection(name, attributes, dig)
        }

        ByteArrayOutputStream().use { baos ->
            manifest.write(baos)

            zip.openEntry(JarFile.MANIFEST_NAME)
            zip.writeEntry(baos.toByteArray(), baos.size().toLong())
            zip.closeEntry()
        }
        val manifestHash = getManifestHash(manifest, dig)
        val tmpManifest = Manifest().apply {
            mainAttributes.putAll(attrs)
        }
        val manifestMainHash = getManifestHash(tmpManifest, dig)
        manifest = Manifest()
        attrs = manifest.mainAttributes
        attrs[Attributes.Name.SIGNATURE_VERSION] = "1.0"
        attrs[Attributes.Name("Created-By")] = "Aliucord Installer"
        attrs[Attributes.Name("SHA1-Digest-Manifest")] = manifestHash
        attrs[Attributes.Name("SHA1-Digest-Manifest-Main-Attributes")] = manifestMainHash
        sectionDigests.forEach { (key, value) ->
            val attributes = Attributes()
            attributes[digestAttr] = value
            manifest.entries[key] = attributes
        }
        var sigBytes: ByteArray
        ByteArrayOutputStream().use { sigStream ->
            manifest.write(sigStream)
            sigBytes = sigStream.toByteArray()
            zip.openEntry("META-INF/CERT.SF")
            zip.writeEntry(sigBytes, sigStream.size().toLong())
            zip.closeEntry()
        }
        val signature = signSigFile(keySet, sigBytes)
        zip.openEntry("META-INF/CERT.RSA")
        zip.writeEntry(signature, signature.size.toLong())
        zip.closeEntry()
        zip.close()
    }

    @Throws(IOException::class)
    private fun hashEntrySection(name: String, attrs: Attributes, dig: MessageDigest): String {
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"

        ByteArrayOutputStream().use { baos ->
            manifest.write(baos)

            val emptyLen = baos.toByteArray().size
            manifest.entries[name] = attrs
            baos.reset()
            manifest.write(baos)
            var ob = baos.toByteArray()
            ob = ob.copyOfRange(emptyLen, ob.size)
            return dig.digest(ob).toBase64()
        }
    }

    @Throws(IOException::class)
    private fun getManifestHash(manifest: Manifest, dig: MessageDigest) = ByteArrayOutputStream().use { baos ->
        manifest.write(baos)
        dig.digest(baos.toByteArray()).toBase64()
    }

    private fun signSigFile(keySet: KeySet, content: ByteArray): ByteArray {
        val sha1Signer = JcaContentSignerBuilder("SHA1withRSA").build(keySet.privateKey)
        val jcaDigestCalculatorProviderBuilder = JcaDigestCalculatorProviderBuilder()
        val jcaSignerInfoGeneratorBuilder =
            JcaSignerInfoGeneratorBuilder(jcaDigestCalculatorProviderBuilder.build()).apply {
                setDirectSignature(true)
            }
        val sigData = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(jcaSignerInfoGeneratorBuilder.build(sha1Signer, keySet.publicKey))
            addCertificates(JcaCertStore(listOf(keySet.publicKey)))
        }.generate(CMSProcessableByteArray(content), false)
        return sigData.toASN1Structure().getEncoded("DER")
    }

    private fun ByteArray.toBase64() = String(Base64.encode(this))
}