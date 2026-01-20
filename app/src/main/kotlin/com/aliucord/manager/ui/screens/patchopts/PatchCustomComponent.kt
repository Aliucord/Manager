package com.aliucord.manager.ui.screens.patchopts

import com.aliucord.manager.network.utils.SemVer
import java.io.File
import kotlin.time.Instant

/**
 * A custom component that was deployed to this device with
 * the `deployWithAdb` task and imported by Manager.
 */
data class PatchCustomComponent(
    val file: File,
    val version: SemVer,
    val buildTime: Instant,
)
