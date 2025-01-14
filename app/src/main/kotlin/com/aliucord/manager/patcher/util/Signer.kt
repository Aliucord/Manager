/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.patcher.util

import com.aliucord.manager.manager.PathManager
import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale

object Signer : KoinComponent {
    private val paths = get<PathManager>()
    private val password = "password".toCharArray()

    private val signerConfig: ApkSigner.SignerConfig by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        // Create new keystore if it doesn't exist
        if (!paths.keystoreFile.exists()) {
            paths.aliucordDir.mkdirs()
            newKeystore(paths.keystoreFile)
        }

        paths.keystoreFile.inputStream()
            .use { keyStore.load(it, /* password = */ null) }

        val alias = keyStore.aliases().nextElement()
        val certificate = keyStore.getCertificate(alias) as X509Certificate

        ApkSigner.SignerConfig.Builder(
            "Aliucord Manager signer",
            KeyConfig.Jca(keyStore.getKey(alias, password) as PrivateKey),
            listOf(certificate)
        ).build()
    }

    private fun newKeystore(out: File) {
        val key = createKey()

        with(KeyStore.getInstance(KeyStore.getDefaultType())) {
            load(null, password)
            setKeyEntry("alias", key.privateKey, password, arrayOf<Certificate>(key.publicKey))
            store(out.outputStream(), password)
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
            /* issuer = */ x500Name,
            /* serial = */ serialNumber,
            /* notBefore = */ Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L),
            /* notAfter = */ Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L),
            /* dateLocale = */ Locale.ENGLISH,
            /* subject = */ x500Name,
            /* publicKeyInfo = */ SubjectPublicKeyInfo.getInstance(pair.public.encoded)
        )
        val signer = JcaContentSignerBuilder("SHA1withRSA").build(pair.private)

        return KeySet(JcaX509CertificateConverter().getCertificate(builder.build(signer)), pair.private)
    }

    fun signApk(apkFile: File) {
        val tmpApk = apkFile.resolveSibling(apkFile.name + ".tmp")

        ApkSigner.Builder(listOf(signerConfig))
            .setV1SigningEnabled(false) // TODO: enable so api <24 devices can work, however zip-alignment breaks
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setInputApk(apkFile)
            .setOutputApk(tmpApk)
            .build()
            .sign()

        tmpApk.renameTo(apkFile)
    }

    private class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)
}
