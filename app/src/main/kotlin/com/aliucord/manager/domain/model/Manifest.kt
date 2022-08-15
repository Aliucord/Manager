/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Manifest(
    val name: String,
    val authors: ArrayList<Author>,
    val description: String,
    val version: String,
    val updateUrl: String,
    val changelog: String?,
    val changelogMedia: String?,
    val links: JsonObject
) {
    val repositoryUrl = updateUrl
        .replace("raw.githubusercontent.com", "github.com")
        .replaceFirst("/builds.*".toRegex(), "")

    @Serializable
    data class Author(val name: String, val id: Long)
}
