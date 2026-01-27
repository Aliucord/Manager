package com.aliucord.manager.patcher

import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import kotlinx.serialization.Serializable

/**
 * Data stored inside patched APKs as "aliucord.json" in order to preserve install-time information about Aliucord and the Manager.
 */
@Serializable
data class InstallMetadata(
    /**
     * Whether the manager is a real release or built from source.
     */
    val customManager: Boolean,

    /**
     * The semver version of this manager that performed the installation.
     */
    val managerVersion: SemVer,

    /**
     * Version (commit hash) of the Aliuhook build that was injected into the APK.
     */
    val aliuhookVersion: SemVer,

    /**
     * Version of the injector build that was injected into the APK.
     */
    val injectorVersion: SemVer,

    /**
     * Version of the smali patches that were applied onto the APK.
     */
    val patchesVersion: SemVer,

    /**
     * Version of the Kotlin stdlib that was injected into the end of the APK.
     */
    val kotlinVersion: SemVer,

    /**
     * The user-selected options for this installation.
     */
    val options: PatchOptions,
)
