package com.aliucord.manager.patcher.steps.download

import android.app.Application
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
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
    private val application: Application by inject()

    /**
     * The target APK file which can be modified during patching
     */
    val patchedApk: File = paths.patchedApk()

    override val group = StepGroup.Download
    override val localizedName = R.string.patch_step_copy_deps

    override suspend fun execute(container: StepRunner) {
        val srcApk = container.getStep<DownloadDiscordStep>().targetFile
        val dir = paths.patchingWorkingDir()

        container.log("Clearing patched directory")
        if (!dir.deleteRecursively())
            throw Error("Failed to clear existing patched dir")

        // Preallocate space for file copy and future patching operations
        if (Build.VERSION.SDK_INT >= 26) {
            val storageManager = application.getSystemService<StorageManager>()!!
            val targetFileStorageId = storageManager.getUuidForPath(patchedApk)
            val fileSize = srcApk.length()

            // We request 3.5x the size of the APK, to give space for the following:
            // 1) A copy of the APK
            // 2) Modifying the copied APK (whether this is necessary I'm not sure)
            // 2) Extracting native libs and other various operations
            val allocSize = (fileSize * 3.5).toLong()
            storageManager.allocateBytes(targetFileStorageId, allocSize)
        }

        container.log("Copying patched apk from ${srcApk.absolutePath} to ${patchedApk.absolutePath}")
        srcApk.copyTo(patchedApk)
    }
}
