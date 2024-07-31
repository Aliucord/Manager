package com.aliucord.manager.installer.steps.patch

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installer.InstallMetadata
import com.aliucord.manager.installer.StepRunner
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.*
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.util.IS_CUSTOM_BUILD
import com.github.diamondminer88.zip.ZipWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Store the install options and additional data inside the APK for future use,
 * for example checking what library versions were used, or performing "updates" while
 * maintaining the same install options as what was used upon first install.
 */
class SaveMetadataStep(private val options: InstallOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_save_metadata

    private val json = Json {
        prettyPrint = true
    }

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val aliuhook = container.getStep<DownloadAliuhookStep>()
        val injector = container.getStep<DownloadInjectorStep>()

        val metadata = InstallMetadata(
            options = options,
            managerVersionName = BuildConfig.VERSION_NAME,
            customManager = IS_CUSTOM_BUILD,
            aliuhookVersion = aliuhook.targetVersion,
            injectorCommitHash = injector.aliucordHash,
        )

        ZipWriter(apk, /* append = */ true).use {
            it.writeEntry("aliucord.json", json.encodeToString<InstallMetadata>(metadata))
        }
    }
}
