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
import com.github.diamondminer88.zip.ZipWriter
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Store the install options and additional data inside the APK for future use,
 * for example checking what library versions were used, or performing "updates" while
 * maintaining the same install options as what was used upon first install.
 */
class SaveMetadataStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val json: Json by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_save_metadata

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val aliuhook = container.getStep<DownloadAliuhookStep>()
        val injector = container.getStep<DownloadInjectorStep>()
        val patches = container.getStep<DownloadPatchesStep>()
        val kotlin = container.getStep<DownloadKotlinStep>()

        val metadata = InstallMetadata(
            options = options,
            customManager = !BuildConfig.RELEASE,
            managerVersion = SemVer.parse(BuildConfig.VERSION_NAME),
            aliuhookVersion = aliuhook.getVersion(container),
            injectorVersion = injector.getVersion(container),
            patchesVersion = patches.getVersion(container),
            kotlinVersion = kotlin.getVersion(container),
        )

        container.log("Writing serialized install metadata to APK")
        ZipWriter(apk, /* append = */ true).use {
            it.writeEntry("aliucord.json", json.encodeToString<InstallMetadata>(metadata))
        }
    }
}
