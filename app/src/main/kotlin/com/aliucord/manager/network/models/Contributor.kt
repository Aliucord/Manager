package com.aliucord.manager.network.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Contributor(
    val username: String,
    val avatarUrl: String,
    val contributions: Int,
)
