/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.models.github

import kotlinx.serialization.Serializable

@Serializable
data class Version(
    val versionCode: String,
    val versionName: String,
    val aliucordHash: String
)
