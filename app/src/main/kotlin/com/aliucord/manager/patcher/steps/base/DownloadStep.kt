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
import com.aliucord.manager.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

@Stable
abstract class DownloadStep<IVersion> : Step(), KoinComponent {
    private val context: Context by inject()
    private val downloader: KtorDownloadManager by inject()

    /**
     * The version of the file that is/will be downloaded.
     * The return type is dynamic as different dependencies have varying version formats.
     */
    abstract fun getVersion(container: StepRunner): IVersion

    /**
     * The remote url to be downloaded to the [getStoredFile].
     */
    abstract fun getRemoteUrl(container: StepRunner): String

    /**
     * Target path to store the download in. If this file already exists at the time
     * of execution, then the cached version is used and the step is marked as skipped.
     */
    abstract fun getStoredFile(container: StepRunner): File

    /**
     * Verify that the download completely successfully without errors.
     * @throws Throwable If verification fails.
     */
    @CallSuper
    open suspend fun verify(container: StepRunner) {
        val file = getStoredFile(container)

        if (!file.exists())
            throw Error("Downloaded file is missing!")
        if (file.length() <= 0)
            throw Error("Downloaded file is empty!")
    }

    override val group = StepGroup.Download

    override suspend fun execute(container: StepRunner) {
        val version = getVersion(container)
        val file = getStoredFile(container)
        val url = getRemoteUrl(container)

        container.log("Checking if file cached: ${file.absolutePath}")
        if (file.exists()) {
            container.log("File exists, verifying...")

            try {
                verify(container)
                state = StepState.Skipped
                container.log("File verified, skipping download")
                return
            } catch (t: Throwable) {
                file.delete()
                container.log("Verification error: " + Log.getStackTraceString(t))
                container.log("File failed verification, deleting and redownloading")
            }
        }

        container.log("Downloading file version: $version at url: $url")
        var lastLogProgress = 0f
        val result = downloader.download(url, file) { newProgress ->
            progress = newProgress ?: -1f

            newProgress?.let { newProgress ->
                if (newProgress > lastLogProgress + 0.1f) {
                    container.log("Download progress: ${(newProgress * 100.0).toPrecision(0)}% after ${getDuration()}ms")
                }
                @Suppress("AssignedValueIsNeverRead") // incorrect
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
                    verify(container)
                } catch (e: CancellationException) {
                    file.delete()
                    throw e
                } catch (t: Throwable) {
                    mainThread { context.showToast(R.string.installer_dl_verify_fail) }
                    container.log("Failed to verify file, deleting...")
                    file.delete()
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
