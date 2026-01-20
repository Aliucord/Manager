package com.aliucord.manager.patcher.steps.download

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.android.apksig.ApkVerifier
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * If not already cached, then download the raw unmodified v126.21 (Kotlin) Discord APK
 * from a redirect to an APK mirror site provided by the Aliucord backend.
 */
@Stable
class DownloadDiscordStep : DownloadStep<Int>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_kt_apk

    override fun getVersion(container: StepRunner) = DISCORD_KT_VERSION
    override fun getRemoteUrl(container: StepRunner) = getDiscordApkUrl(DISCORD_KT_VERSION)
    override fun getStoredFile(container: StepRunner) = paths.cachedDiscordApk(DISCORD_KT_VERSION)

    override suspend fun verify(container: StepRunner) {
        super.verify(container)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            container.log("Verifying APK signature")
            verifySignature(getStoredFile(container))
        } else {
            container.log("Skipping APK signature verification, API level too old")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P) // Requires Type#getTypeName()
    private fun verifySignature(apk: File) {
        // Verify the signature of the APK to ensure it's the original
        val verifier = ApkVerifier.Builder(apk).build()
        val result = try {
            verifier.verify()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to verify APK! It may have been corrupted or tampered with.", e)
        }

        if (!result.isVerified)
            throw SignatureVerificationException(result.allErrors)

        if (result.signerCertificates.singleOrNull()
                ?.let { it.encoded.toByteString().sha256() == DISCORD_CERTIFICATE_SHA256.decodeHex() } != true
        ) {
            throw VerifyError("Failed to verify Discord's APK signatures! This is an unoriginal APK that has been tampered with.")
        }
    }

    private companion object {
        /**
         * Last version of Discord before the RNA rewrite
         */
        const val DISCORD_KT_VERSION = 126021

        /**
         * The sha256 of the certificate that is used to sign Discord APKs.
         *
         * ```shell
         * $ apksigner verify --print-certs discord-126021.apk
         *
         * Signer #1 certificate DN: CN=Jason Citron, O=Hammer and Chisel, L=Burlingame, ST=CA, C=US
         * Signer #1 certificate SHA-256 digest: 3c39d23cf9367849a5c699395647fe0e5bfea5a1f1f40d8c717ddc70f8bfa113
         * Signer #1 certificate SHA-1 digest: b07fc6aeccd21fcbd40543c85112cafe099ba56f
         * Signer #1 certificate MD5 digest: 7b5946851247df3a8f8c501bb0d34cd9
         * Source Stamp Signer certificate DN: CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US
         * Source Stamp Signer certificate SHA-256 digest: 3257d599a49d2c961a471ca9843f59d341a405884583fc087df4237b733bbd6d
         * Source Stamp Signer certificate SHA-1 digest: b1af3a0bf998aeede1a8716a539e5a59da1d86d6
         * Source Stamp Signer certificate MD5 digest: 577b8a9fbc7e308321aec6411169d2fb
         * ```
         */
        const val DISCORD_CERTIFICATE_SHA256 = "3c39d23cf9367849a5c699395647fe0e5bfea5a1f1f40d8c717ddc70f8bfa113"

        fun getDiscordApkUrl(version: Int) =
            "${BuildConfig.MAVEN_URL}/com/discord/discord/$version/discord-$version.apk"
    }

    private class SignatureVerificationException(errors: List<ApkVerifier.IssueWithParams>) : Exception(
        "Failed to verify APK signatures! " +
            "This is an unoriginal APK that has been tampered with. " +
            "Verification errors: " + errors.joinToString()
    )
}
