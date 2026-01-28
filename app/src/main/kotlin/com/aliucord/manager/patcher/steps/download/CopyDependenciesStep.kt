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
import com.aliucord.manager.patcher.util.InsufficientStorageException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException

/**
 * Step to duplicate the Discord APK to be worked on.
 */
class CopyDependenciesStep : Step(), KoinComponent {
    private val paths: PathManager by inject()
    private val application: Application by inject()

    /**
     * The target APK file that will be modified during patching.
     */
    val apk: File = paths.patchedApk

    override val group = StepGroup.Download
    override val localizedName = R.string.patch_step_copy_deps

    override suspend fun execute(container: StepRunner) {
        val srcApk = container.getStep<DownloadDiscordStep>().getStoredFile(container)

        container.log("Clearing patched directory")
        if (!paths.patchingWorkingDir.deleteRecursively())
            throw Error("Failed to clear existing patched dir")

        // Preallocate space for file copy and future patching operations
        if (Build.VERSION.SDK_INT >= 26) {
            val storageManager = application.getSystemService<StorageManager>()!!
            val targetFileStorageId = storageManager.getUuidForPath(apk)
            val fileSize = srcApk.length()

            // We request 3.5x the size of the APK, to give space for the following:
            // 1) A copy of the APK
            // 2) Modifying the copied APK (whether this is necessary I'm not sure)
            // 2) Extracting native libs and other various operations
            val allocSize = (fileSize * 3.5).toLong()

            try {
                storageManager.allocateBytes(targetFileStorageId, allocSize)
            } catch (e: IOException) {
                throw InsufficientStorageException(e.message)
            }
        }

        container.log("Copying patched apk from ${srcApk.absolutePath} to ${apk.absolutePath}")
        apk.parentFile!!.mkdirs()
        srcApk.copyTo(apk)
    }
}
