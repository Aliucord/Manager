package com.aliucord.manager.installer.steps

import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.*
import com.aliucord.manager.installer.steps.install.*
import com.aliucord.manager.installer.steps.patch.*
import com.aliucord.manager.installer.steps.prepare.DowngradeCheckStep
import com.aliucord.manager.installer.steps.prepare.FetchInfoStep
import kotlinx.collections.immutable.persistentListOf

/**
 * Used for installing the old Kotlin Discord app.
 */
class KotlinInstallRunner : StepRunner() {
    override val steps = persistentListOf<Step>(
        // Prepare
        FetchInfoStep(),
        DowngradeCheckStep(),

        // Download
        DownloadDiscordStep(),
        DownloadInjectorStep(),
        DownloadAliuhookStep(),
        DownloadKotlinStep(),

        // Patch
        CopyDependenciesStep(),
        ReplaceIconStep(),
        PatchManifestStep(),
        AddInjectorStep(),
        AddAliuhookStep(),

        // Install
        AlignmentStep(),
        SigningStep(),
        InstallStep(),
        CleanupStep(),
    )
}
