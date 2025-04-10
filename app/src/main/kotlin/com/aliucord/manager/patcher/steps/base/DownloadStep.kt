package com.aliucord.manager.patcher.steps.base

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.download.IDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.util.showToast
import com.aliucord.manager.util.toPrecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

@Stable
abstract class DownloadStep : Step(), KoinComponent {
    private val context: Context by inject()
    private val downloader: KtorDownloadManager by inject()

    /**
     * The remote url to download
     */
    abstract val targetUrl: String

    /**
     * Target path to store the download in. If this file already exists,
     * then the cached version is used and the step is marked as cancelled/skipped.
     */
    abstract val targetFile: File

    /**
     * Verify that the download completely successfully without errors.
     * @throws Throwable If verification fails.
     */
    @CallSuper
    open suspend fun verify() {
        if (!targetFile.exists())
            throw Error("Downloaded file is missing!")

        if (targetFile.length() <= 0)
            throw Error("Downloaded file is empty!")
    }

    override val group = StepGroup.Download

    override suspend fun execute(container: StepRunner) {
        container.log("Checking if file cached: ${targetFile.absolutePath}")

        if (targetFile.exists()) {
            container.log("File exists, verifying...")

            try {
                verify()
                state = StepState.Skipped
                container.log("File verified, skipping download")
                return
            } catch (t: Throwable) {
                targetFile.delete()
                container.log("Verification error: " + Log.getStackTraceString(t))
                container.log("File failed verification, deleting and redownloading")
            }
        }

        container.log("Downloading file at url: $targetUrl")
        var lastLogProgress = 0f
        val result = downloader.download(targetUrl, targetFile) { newProgress ->
            progress = newProgress ?: -1f

            if (newProgress != null && newProgress > lastLogProgress + 0.1f) {
                container.log("Download progress: ${(newProgress * 100.0).toPrecision(0)}% after ${getDuration()}ms")
                lastLogProgress = newProgress
            }
        }

        when (result) {
            is IDownloadManager.Result.Cancelled -> {
                state = StepState.Error
                container.log("Download cancelled!")
            }

            is IDownloadManager.Result.Success -> {
                container.log("Successfully downloaded file, verifying...")

                try {
                    verify()
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        context.showToast(R.string.installer_dl_verify_fail)
                    }

                    container.log("Failed to verify file, deleting...")
                    targetFile.delete()
                    throw t
                }

                container.log("Verified downloaded file")
            }

            is IDownloadManager.Result.Error -> {
                withContext(Dispatchers.Main) {
                    val toastText = result.getLocalizedReason(context)
                        ?: context.getString(R.string.downloader_err_unknown)

                    Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                }

                container.log("Failed to download file")
                throw Error("Failed to download: $result")
            }
        }
    }
}
