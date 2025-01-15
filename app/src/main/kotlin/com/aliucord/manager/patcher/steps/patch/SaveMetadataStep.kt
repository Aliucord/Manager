package com.aliucord.manager.patcher.steps.patch

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.InstallMetadata
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.*
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.util.IS_CUSTOM_BUILD
import com.github.diamondminer88.zip.ZipWriter
import kotlinx.serialization.json.Json

/**
 * Store the install options and additional data inside the APK for future use,
 * for example checking what library versions were used, or performing "updates" while
 * maintaining the same install options as what was used upon first install.
 */
class SaveMetadataStep(private val options: PatchOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_save_metadata

    private val json = Json {
        prettyPrint = true
    }

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val aliuhook = container.getStep<DownloadAliuhookStep>()
        val injector = container.getStep<DownloadInjectorStep>()
        val patches = container.getStep<DownloadPatchesStep>()

        val metadata = InstallMetadata(
            options = options,
            customManager = IS_CUSTOM_BUILD,
            managerVersion = SemVer.parse(BuildConfig.VERSION_NAME),
            aliuhookVersion = aliuhook.targetVersion,
            injectorVersion = injector.targetVersion,
            patchesVersion = patches.targetVersion,
        )

        ZipWriter(apk, /* append = */ true).use {
            it.writeEntry("aliucord.json", json.encodeToString<InstallMetadata>(metadata))
        }
    }
}
