package com.aliucord.manager.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import java.io.Serializable

@Immutable
sealed interface DiscordVersion : Comparable<DiscordVersion> {
    data object Error : DiscordVersion
    data object None : DiscordVersion

    data class Existing(
        val type: Type,
        val name: String,
        val code: Int,
    ) : DiscordVersion, Serializable {
        val rawCode: Int
            get() = (code / 1000 * 100) + code % 100
    }

    override fun compareTo(other: DiscordVersion): Int {
        return when (this) {
            is Error -> 0
            is None -> 0
            is Existing -> {
                if (other is Existing) {
                    other.rawCode.compareTo(rawCode)
                } else {
                    0
                }
            }
        }
    }

    @Composable
    fun toDisplayName() = when (this) {
        is Error -> stringResource(R.string.version_load_fail)
        is None -> stringResource(R.string.version_none)
        is Existing -> when (type) {
            Type.STABLE -> stringResource(R.string.version_stable)
            Type.BETA -> stringResource(R.string.version_beta)
            Type.ALPHA -> stringResource(R.string.version_alpha)
            Type.UNKNOWN -> stringResource(R.string.version_unknown)
        }
    }

    enum class Type {
        STABLE,
        BETA,
        ALPHA,
        UNKNOWN,
    }

    companion object {
        fun parseVersionType(versionCode: Int?): Type {
            return when (versionCode?.div(100)?.mod(10)) {
                0 -> Type.STABLE
                1 -> Type.BETA
                2 -> Type.ALPHA
                else -> Type.UNKNOWN
            }
        }
    }
}
