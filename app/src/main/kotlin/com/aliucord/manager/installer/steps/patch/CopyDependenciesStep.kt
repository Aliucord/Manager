package com.aliucord.manager.installer.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.download.DownloadDiscordStep
import com.aliucord.manager.manager.PathManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Step to duplicate the Discord APK to be worked on.
 */
class CopyDependenciesStep : Step(), KoinComponent {
    private val paths: PathManager by inject()

    /**
     * The target APK file which can be modified during patching
     */
    val patchedApk: File = paths.patchingWorkingDir()
        .resolve("patched.apk")

    override val group = StepGroup.Patch
    override val localizedName = R.string.install_step_copy

    override suspend fun execute(container: StepRunner) {
        val dir = paths.patchingWorkingDir()

        // TODO: move this to a prepare step
        if (!dir.deleteRecursively())
            throw Error("Failed to clear existing patched dir")

        val srcApk = container.getStep<DownloadDiscordStep>().targetFile

        srcApk.copyTo(patchedApk)
    }
}
