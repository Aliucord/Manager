/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*

private class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)

object Signer {
    private val password = "password".toCharArray()

    private val signerConfig: ApkSigner.SignerConfig by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        aliucordDir.resolve("ks.keystore").also {
            if (!it.exists()) newKeystore(it)
        }.inputStream().use { stream ->
            keyStore.load(stream, null)
        }

        val alias = keyStore.aliases().nextElement()
        val certificate = keyStore.getCertificate(alias) as X509Certificate

        ApkSigner.SignerConfig.Builder(
            "Aliucord Manager signer",
            keyStore.getKey(alias, password) as PrivateKey,
            listOf(certificate),
        ).build()
    }

    private fun newKeystore(out: File?) {
        val key = createKey()

        with(KeyStore.getInstance(KeyStore.getDefaultType())) {
            load(null, password)
            setKeyEntry("alias", key.privateKey, password, arrayOf<Certificate>(key.publicKey))
            store(out?.outputStream(), password)
        }
    }

    private fun createKey(): KeySet {
        var serialNumber: BigInteger

        do serialNumber = SecureRandom().nextInt().toBigInteger()
        while (serialNumber < BigInteger.ZERO)

        val x500Name = X500Name("CN=Aliucord Manager")
        val pair = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }
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

    fun signApk(apkFile: File) {
        val outputApk = aliucordDir.resolve("Aliucord-Signed.apk")
        ApkSigner.Builder(listOf(signerConfig))
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setOtherSignersSignaturesPreserved(false)
            .setInputApk(apkFile)
            .setOutputApk(outputApk)
            .build()
            .sign()
        outputApk.renameTo(apkFile)
    }
}
