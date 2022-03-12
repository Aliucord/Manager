/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.models.github

import com.google.gson.annotations.SerializedName

data class GithubUser(
    @SerializedName("login") val name: String,
    val contributions: Int
)
