/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildInfo(
    @SerialName("versionCode")
    val discordVersionCode: String,
    @SerialName("versionName")
    val discordVersionName: String,
    @SerialName("aliucordHash")
    val injectorVersion: String,
)
