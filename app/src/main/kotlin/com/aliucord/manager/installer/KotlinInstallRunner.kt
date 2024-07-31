package com.aliucord.manager.installer

import com.aliucord.manager.installer.steps.download.*
import com.aliucord.manager.installer.steps.install.*
import com.aliucord.manager.installer.steps.patch.*
import com.aliucord.manager.installer.steps.prepare.DowngradeCheckStep
import com.aliucord.manager.installer.steps.prepare.FetchInfoStep
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import kotlinx.collections.immutable.persistentListOf

/**
 * Used for installing the old Kotlin Discord app.
 */
class KotlinInstallRunner(options: InstallOptions) : StepRunner() {
    override val steps = persistentListOf(
        // Prepare
        FetchInfoStep(),
        DowngradeCheckStep(options),

        // Download
        DownloadDiscordStep(),
        DownloadInjectorStep(),
        DownloadAliuhookStep(),
        DownloadKotlinStep(),
        CopyDependenciesStep(),

        // Patch
        ReplaceIconStep(options),
        PatchManifestStep(options),
        AddInjectorStep(),
        AddAliuhookStep(),
        SaveMetadataStep(options),

        // Install
        AlignmentStep(),
        SigningStep(),
        InstallStep(options),
        CleanupStep(),
    )
}
