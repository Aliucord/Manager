/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.models

data class Manifest(
    val name: String,
    val authors: ArrayList<Author>,
    val description: String,
    val version: String,
    val updateUrl: String,
    val changelog: String?,
    val changelogMedia: String?
) {
    data class Author(val name: String, val id: Long)
}
