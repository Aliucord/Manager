/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Commit(
    @SerialName("html_url")
    val htmlUrl: String,
    val sha: String,
    val commit: Commit,
    val author: Author?
) {
    @Serializable
    data class Author(@SerialName("login") val name: String)
    @Serializable
    data class Commit(val message: String)
}
