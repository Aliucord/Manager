package com.aliucord.manager.patcher.steps.install

import android.app.Application
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.days

// TODO: prompt user to uninstall Aliucord if signing keystore is unavailable/corrupt and CorePatch isn't installed

/**
 * Sign the APK with a keystore generated on-device.
 */
class SigningStep(
    private val options: PatchOptions,
) : Step(), KoinComponent {
    private val paths: PathManager by inject()
    private val context: Application by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_signing

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().apk
        val tmpApk = apk.resolveSibling(apk.name + ".tmp")

        container.log("Building signing config and storing keystore")

        val (keystore, keystoreBytes) = getKeystore(options.packageName)
        val keyAlias = keystore.aliases().nextElement()
        val signingConfig = ApkSigner.SignerConfig.Builder(
            /* name = */ "Aliucord Manager",
            /* keyConfig = */ KeyConfig.Jca(keystore.getKey(keyAlias, LEGACY_KEYSTORE_PASSWORD) as PrivateKey),
            /* certificates = */ listOf(keystore.getCertificate(keyAlias) as X509Certificate)
        ).build()

        ZipWriter(apk, /* append = */ true).use { zip ->
            zip.writeEntry("aliucord.keystore", keystoreBytes)
        }

        container.log("Signing apk at ${apk.absolutePath}")

        ApkSigner.Builder(listOf(signingConfig))
            .setV1SigningEnabled(false) // TODO: enable so api <24 devices can work, however zip-alignment breaks
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setInputApk(apk)
            .setOutputApk(tmpApk)
            .build()
            .sign()

        tmpApk.renameTo(apk)
    }

    /**
     * Attempts to load or generate a signing keystore with various priorities.
     *
     * 1. If the specified Aliucord installation for [packageName] exists,
     *    then attempt to load the signing keystore that was embedded into it.
     * 3. If external storage permissions are granted and the legacy keystore exists,
     *    then move it to Manager's internal storage and use that.
     * 2. If Manager has a signing key already stored in internal storage
     *    (that isn't persisted between reinstallations of Manager) then use that.
     * 4. Otherwise, generate a new keystore in Manager's internal storage.
     *
     * Returns the loaded keystore along with its byte representation.
     */
    private fun getKeystore(packageName: String): Pair<KeyStore, ByteArray> {
        val embeddedKeystoreRaw = try {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            ZipReader(applicationInfo.publicSourceDir)
                .use { it.openEntry("aliucord.keystore")?.read() }
        } catch (_: Exception) {
            null
        }

        if (embeddedKeystoreRaw != null) {
            try {
                val keystore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    load(embeddedKeystoreRaw.inputStream(), LEGACY_KEYSTORE_PASSWORD)
                }
                return keystore to embeddedKeystoreRaw
            } catch (e: Exception) {
                throw Exception("Embedded existing signing key is corrupted! Please uninstall Aliucord and retry!", e)
            }
        }

        try {
            if (paths.legacyKeystoreFile.exists()) {
                paths.legacyKeystoreFile.copyTo(paths.keystoreFile, overwrite = true)
                paths.legacyKeystoreFile.delete()
            }
        } catch (_: SecurityException) {
            // Ignore
        }

        if (!paths.keystoreFile.exists()) {
            createKeystore(LEGACY_KEYSTORE_PASSWORD)
                .also { paths.keystoreFile.writeBytes(it) }
        }

        val keystoreBytes = paths.keystoreFile.readBytes()
        val keystore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(keystoreBytes.inputStream(), LEGACY_KEYSTORE_PASSWORD)
        }
        return keystore to keystoreBytes
    }

    /**
     * Creates a keystore with a new keyset and protects it with a password.
     */
    private fun createKeystore(password: CharArray): ByteArray {
        // Generate keys + certificate
        val keys = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }
        val signer = JcaContentSignerBuilder("SHA1withRSA")
            .build(keys.private)
        val certificate = X509v3CertificateBuilder(
            /* issuer = */ X500Name("CN=Aliucord Manager"),
            /* serial = */ abs(Random().nextInt()).toBigInteger(),
            /* notBefore = */ Date(System.currentTimeMillis() - 365.days.inWholeMilliseconds),
            /* notAfter = */ Date(System.currentTimeMillis() + (100 * 365.days.inWholeMilliseconds)),
            /* dateLocale = */ Locale.ENGLISH,
            /* subject = */ X500Name("CN=Aliucord Manager"),
            /* publicKeyInfo = */ SubjectPublicKeyInfo.getInstance(keys.public.encoded),
        ).build(signer)

        val publicKey = JcaX509CertificateConverter().getCertificate(certificate)
        val privateKey = keys.private

        val keystoreBytes = ByteArrayOutputStream()
        val keystore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, password)
            setKeyEntry(
                /* alias = */ "alias",
                /* key = */ privateKey,
                /* password = */ password,
                /* chain = */ arrayOf(publicKey),
            )
        }
        keystore.store(keystoreBytes, password)
        return keystoreBytes.toByteArray()
    }

    private companion object {
        // TODO: Figure out a way to get a unique and private key/identifier that is only available to Manager
        //       and is persistable across multiple installations.

        /**
         * This password was used to secure the old keystore stored in external storage at
         * `/storage/emulated/0/Aliucord/ks.keystore`
         */
        val LEGACY_KEYSTORE_PASSWORD = "password".toCharArray()
    }
}
