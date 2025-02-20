package com.aliucord.manager.patcher.steps.base

import android.content.Context
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.download.IDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.util.showToast
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
        if (targetFile.exists()) {
            if (targetFile.length() > 0) {
                state = StepState.Skipped
                return
            }

            targetFile.delete()
        }

        val result = downloader.download(targetUrl, targetFile) { newProgress ->
            progress = newProgress ?: -1f
        }

        when (result) {
            is IDownloadManager.Result.Cancelled ->
                state = StepState.Error

            is IDownloadManager.Result.Success -> {
                try {
                    verify()
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        context.showToast(R.string.installer_dl_verify_fail)
                    }

                    targetFile.delete()
                    throw t
                }
            }

            is IDownloadManager.Result.Error -> {
                withContext(Dispatchers.Main) {
                    val toastText = result.getLocalizedReason(context)
                        ?: context.getString(R.string.downloader_err_unknown)

                    Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                }

                throw Error("Failed to download: $result")
            }
        }
    }
}
