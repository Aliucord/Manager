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
     * Aliuhook release version injected into the APK.
     */
    val aliuhookVersion: String,

    /**
     * Short commit hash of the commit the injector was built from.
     */
    val injectorCommitHash: String,
)
