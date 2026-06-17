/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.network.models

import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.util.serialization.IntAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote Aliucord build data available at https://builds.aliucord.com/data.json
 * This is used to determine the latest available versions of components.
 */
@Serializable
data class BuildInfo(
    @Serializable(with = IntAsStringSerializer::class)
    @SerialName("versionCode")
    val discordVersionCode: Int,
    // @SerialName("versionName")
    // val discordVersionName: Int,
    // @SerialName("coreVersion")
    // val coreVersion: SemVer,
    @SerialName("injectorVersion")
    val injectorVersion: SemVer,
    @SerialName("patchesVersion")
    val patchesVersion: SemVer,
    @SerialName("kotlinVersion")
    val kotlinVersion: SemVer,
)
