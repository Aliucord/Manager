package com.aliucord.manager.network.utils

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Parses a Semantic version in the format of `v1.0.0` or `1.0.0`.
 * This always gets serialized and stringified without the `v` prefix.
 */
@Immutable
@Parcelize
@Serializable(SemVer.Serializer::class)
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer>, Parcelable {
    override fun compareTo(other: SemVer): Int {
        var cmp = 0
        if (0 != major.compareTo(other.major).also { cmp = it })
            return cmp
        if (0 != minor.compareTo(other.minor).also { cmp = it })
            return cmp
        if (0 != patch.compareTo(other.patch).also { cmp = it })
            return cmp

        return 0
    }

    override fun equals(other: Any?): Boolean {
        val ver = other as? SemVer
            ?: return false

        return ver.major == major &&
            ver.minor == minor &&
            ver.patch == patch
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    companion object {
        fun parse(version: String): SemVer = parseOrNull(version)
            ?: throw IllegalArgumentException("Invalid semver string $version")

        fun parseOrNull(version: String): SemVer? {
            val parts = version.removePrefix("v").split(".")

            if (parts.size != 3)
                return null

            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null

            return SemVer(major, minor, patch)
        }
    }

    object Serializer : KSerializer<SemVer> {
        override val descriptor = PrimitiveSerialDescriptor("SemVer", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) =
            parse(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: SemVer) {
            encoder.encodeString(value.toString())
        }
    }
}
