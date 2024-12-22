package com.aliucord.manager.network.utils

import androidx.compose.runtime.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Parses a Semantic version in the format of `v1.0.0` (v[major].[minor].[patch])
 */
@Immutable
@Serializable(SemVer.Serializer::class)
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    private val vPrefix: Boolean = false,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        val pairs = arrayOf(
            major to other.major,
            minor to other.minor,
            patch to other.patch
        )

        return pairs
            .map { (first, second) -> first.compareTo(second) }
            .find { it != 0 }
            ?: 0
    }

    override fun equals(other: Any?): Boolean {
        val ver = other as? SemVer
            ?: return false

        return ver.major == major &&
            ver.minor == minor &&
            ver.patch == patch
    }

    override fun toString(): String {
        return "${if (vPrefix) "v" else ""}$major.$minor.$patch"
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
            val versionStr = version.removePrefix("v")
            val parts = versionStr
                .split(".")
                .mapNotNull { it.toIntOrNull() }
                .takeIf { it.size == 3 }
                ?: return null

            return SemVer(parts[0], parts[1], parts[2], version[0] == 'v')
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
