/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.installer.signing

import android.os.Build
import com.aliucord.manager.aliucordDir
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

object Signer {
    private val password = "password".toCharArray()

    private val keySet: KeySet by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        aliucordDir.resolve("ks.keystore").also {
            if (!it.exists()) {
                aliucordDir.mkdir()
                createKeyStore(it)
            }
        }.inputStream().use { stream ->
            keyStore.load(stream, null)
        }

        val alias = keyStore.aliases().nextElement()

        KeySet(
            publicKey = keyStore.getCertificate(alias) as X509Certificate,
            privateKey = keyStore.getKey(alias, password) as PrivateKey,
        )
    }

    private fun createKeyStore(out: File) {
        out.parentFile?.mkdirs()
        out.delete()

        val key = createKeySet()
        with(KeyStore.getInstance(KeyStore.getDefaultType())) {
            load(null, password)
            setKeyEntry("alias", key.privateKey, password, arrayOf<Certificate>(key.publicKey))
            store(out.outputStream(), password)
        }
    }

    private fun createKeySet(): KeySet {
        var serialNumber: BigInteger

        do serialNumber = SecureRandom().nextInt().toBigInteger()
        while (serialNumber < BigInteger.ZERO)

        val x500Name = X500Name("CN=Aliucord Manager")
        val pair = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }
        val builder = X509v3CertificateBuilder(
            x500Name, // issuer
            serialNumber, // serial
            Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L), // notBefore
            Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L), // notAfter
            Locale.ENGLISH, // dateLocale
            x500Name, // subject
            SubjectPublicKeyInfo.getInstance(pair.public.encoded), // publicKeyInfo
        )
        val signer = JcaContentSignerBuilder("SHA1withRSA").build(pair.private)

        return KeySet(
            publicKey = JcaX509CertificateConverter()
                .getCertificate(builder.build(signer)),
            privateKey = pair.private
        )
    }

    fun signApk(apkFile: File) {
        val isAndroid7 = Build.VERSION.SDK_INT >= 24

        if (isAndroid7) {
            val config = ApkSigner.SignerConfig.Builder(
                "Aliucord Manager",
                keySet.privateKey,
                listOf(keySet.publicKey)
            ).build()

            val tmpFile = aliucordDir.resolve(apkFile.name)
            ApkSigner.Builder(listOf(config))
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setInputApk(apkFile)
                .setOutputApk(tmpFile)
                .build()
                .sign()

            tmpFile.renameTo(apkFile)
        } else {
            V1Signer.signApk(apkFile, keySet)
        }
    }
}
