package com.aliucord.manager.patcher

import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import kotlinx.serialization.Serializable

/**
 * Data stored inside patched APKs as "aliucord.json" in order to preserve install-time information about Aliucord and the Manager.
 */
@Serializable
data class InstallMetadata(
    /**
     * The user-selected options for this installation.
     */
    val options: PatchOptions,

    /**
     * The semver version of this manager that performed the installation.
     */
    val managerVersionName: String,

    /**
     * Whether the manager is a real release or built from source.
     */
    val customManager: Boolean,

    /**
     * Version of the Aliucord release build that was injected into the APK.
     */
    val aliuhookVersion: String,

    /**
     * Version of the injector build that was injected into the APK.
     */
    val injectorVersion: String,
)
