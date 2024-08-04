package com.aliucord.manager.patcher.steps.download

import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
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

    override val group = StepGroup.Download
    override val localizedName = R.string.patch_step_copy_deps

    override suspend fun execute(container: StepRunner) {
        val dir = paths.patchingWorkingDir()

        // TODO: move this to a prepare step
        if (!dir.deleteRecursively())
            throw Error("Failed to clear existing patched dir")

        val srcApk = container.getStep<DownloadDiscordStep>().targetFile

        srcApk.copyTo(patchedApk)
    }
}
