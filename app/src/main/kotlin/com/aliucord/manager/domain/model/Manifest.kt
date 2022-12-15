/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Manifest(
    val name: String,
    val authors: ArrayList<Author>,
    val description: String,
    val version: String,
    val updateUrl: String,
    val changelog: String?,
    val changelogMedia: String?,
) {
    val repositoryUrl = updateUrl
        .replace("raw.githubusercontent.com", "github.com")
        .replaceFirst("/builds.*".toRegex(), "")

    @Serializable
    data class Author(val name: String, val id: Long)
}
