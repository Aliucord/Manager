/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.network.models

import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.util.serialization.IntAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildInfo(
    @Serializable(with = IntAsStringSerializer::class)
    @SerialName("versionCode")
    val discordVersionCode: Int,

    // @SerialName("versionName")
    // val discordVersionName: Int,

    @SerialName("injectorVersion")
    val injectorVersion: SemVer,

    @SerialName("patchesVersion")
    val patchesVersion: SemVer,
)
