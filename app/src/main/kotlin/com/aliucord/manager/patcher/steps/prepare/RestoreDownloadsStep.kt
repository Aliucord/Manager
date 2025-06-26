package com.aliucord.manager.patcher.steps.prepare

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Restores downloaded files necessary for patching from the cache dir.
 * Refer to [PathManager.patchingDownloadDir] and [PathManager.cacheDownloadDir]
 * for more information.
 */
@Stable
class RestoreDownloadsStep : Step(), KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_restore_cache

    override suspend fun execute(container: StepRunner) {
        if (paths.cacheDownloadDir.exists()) {
            container.log("Moving downloads from cache to permanent storage")
            paths.patchingDownloadDir.deleteRecursively()
            paths.cacheDownloadDir.renameTo(paths.patchingDownloadDir)
        } else {
            container.log("No download cache present")
            state = StepState.Skipped
        }
    }
}
