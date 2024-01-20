package com.aliucord.manager.installer.steps.base

import android.content.Context
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.manager.DownloadManager
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

@Stable
abstract class DownloadStep : Step(), KoinComponent {
    private val context: Context by inject()
    private val downloads: DownloadManager by inject()

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
    open suspend fun verify() {
        if (!targetFile.exists())
            throw Error("Downloaded file is missing!")

        if (targetFile.length() <= 0)
            throw Error("Downloaded file is empty!")
    }

    override val group = StepGroup.Download

    override suspend fun execute(container: StepContainer) {
        if (targetFile.exists()) {
            if (targetFile.length() > 0) {
                state = StepState.Skipped
                return
            }

            targetFile.delete()
        }

        val result = downloads.download(targetUrl, targetFile) { newProgress ->
            progress = newProgress ?: -1f
        }

        when (result) {
            is DownloadManager.Result.Success -> {
                try {
                    verify()
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        context.showToast(R.string.installer_dl_verify_fail)
                    }

                    throw t
                }
            }

            is DownloadManager.Result.Error -> {
                withContext(Dispatchers.Main) {
                    context.showToast(result.localizedReason)
                }

                throw Error("Failed to download: ${result.debugReason}")
            }

            is DownloadManager.Result.Cancelled ->
                state = StepState.Cancelled
        }
    }
}
