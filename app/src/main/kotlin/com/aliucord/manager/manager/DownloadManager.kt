package com.aliucord.manager.manager

import android.app.Application
import android.app.DownloadManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handle downloading remote urls to a path through the system [DownloadManager].
 */
class DownloadManager(application: Application) {
    private val downloadManager = application.getSystemService<DownloadManager>()
        ?: throw IllegalStateException("DownloadManager service is not available")

    /**
     * Start a cancellable download with the system [DownloadManager].
     * If the current [CoroutineScope] is cancelled, then the system download will be cancelled within 100ms.
     * @param url Remote src url
     * @param out Target path to download to. It is assumed that the application has write permissions to this path.
     * @param onProgressUpdate An optional [ProgressListener]
     */
    suspend fun download(
        url: String,
        out: File,
        onProgressUpdate: ProgressListener? = null,
    ): Result {
        onProgressUpdate?.onUpdate(null)
        out.parentFile?.mkdirs()

        // Create and start a download in the system DownloadManager
        val downloadId = DownloadManager.Request(Uri.parse(url))
            .setTitle("Aliucord Manager")
            .setDescription("Downloading ${out.name}...")
            .setDestinationUri(Uri.fromFile(out))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .addRequestHeader("User-Agent", "Aliucord Manager/${BuildConfig.VERSION_NAME}")
            .apply {
                // Disable gzip on emulator due to https compression bug
                println(Build.PRODUCT)
                // if (Build.PRODUCT == "google_sdk") {
                    Log.i(BuildConfig.TAG, "Disabling DownloadManager compression")
                    addRequestHeader("Accept-Encoding", null)
                // }
            }
            .let(downloadManager::enqueue)

        // Repeatedly request download state until it is finished
        while (true) {
            try {
                // Hand over control to a suspend function to check for cancellation
                // At the same time, delay 100ms to slow down the potentially infinite loop
                delay(100)
            } catch (_: CancellationException) {
                // If the running CoroutineScope has been cancelled, then gracefully cancel download
                downloadManager.remove(downloadId)
                return Result.Cancelled(systemTriggered = false)
            }

            // Request download status
            val cursor = DownloadManager.Query()
                .setFilterById(downloadId)
                .let(downloadManager::query)

            cursor.use {
                // No results in cursor, download was cancelled
                if (!cursor.moveToFirst()) {
                    return Result.Cancelled(systemTriggered = true)
                }

                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusColumn)

                when (status) {
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED ->
                        onProgressUpdate?.onUpdate(null)

                    DownloadManager.STATUS_RUNNING ->
                        onProgressUpdate?.onUpdate(getDownloadProgress(cursor))

                    DownloadManager.STATUS_SUCCESSFUL ->
                        return Result.Success(out)

                    DownloadManager.STATUS_FAILED -> {
                        val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonColumn)

                        return Result.Error(reason)
                    }

                    else -> throw Error("Unreachable")
                }
            }
        }
    }

    /**
     * Get the download progress of the current row in a [DownloadManager.Query].
     * @return Download progress in the range of `[0,1]`
     */
    private fun getDownloadProgress(queryCursor: Cursor): Float {
        val bytesColumn = queryCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val bytes = queryCursor.getLong(bytesColumn)

        val totalBytesColumn = queryCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        val totalBytes = queryCursor.getLong(totalBytesColumn)

        if (totalBytes <= 0) return 0f
        return bytes.toFloat() / totalBytes
    }

    /**
     * A callback executed from a coroutine called every 100ms in order to provide
     * info about the current download. This should not perform long-running tasks as the delay will be offset.
     */
    fun interface ProgressListener {
        /**
         * @param progress The current download progress in a `[0,1]` range. If null, then the download is either
         *                 paused, pending, or waiting to retry.
         */
        fun onUpdate(progress: Float?)
    }

    /**
     * The state of a download after execution has been completed and the system-level [DownloadManager] has been cleaned up.
     */
    sealed interface Result {
        /**
         * The download succeeded successfully.
         * @param file The path that the download was downloaded to.
         */
        data class Success(val file: File) : Result

        /**
         * This download was interrupted and the in-progress file has been deleted.
         * @param systemTriggered Whether the cancellation happened from the system (ie. clicked cancel on the download notification)
         *                        Otherwise, this was caused by a coroutine cancellation.
         */
        data class Cancelled(val systemTriggered: Boolean) : Result

        /**
         * Error returned by the system [DownloadManager].
         * @param reason The reason code returned by the [DownloadManager.COLUMN_REASON] column.
         */
        data class Error(private val reason: Int) : Result {
            /**
             * Convert a [DownloadManager.COLUMN_REASON] code into its name.
             */
            val debugReason = when (reason) {
                DownloadManager.ERROR_UNKNOWN -> "Unknown"
                DownloadManager.ERROR_FILE_ERROR -> "File Error"
                DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
                DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
                DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
                DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Target file's device not found"
                DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File exists"
                /* DownloadManager.ERROR_BLOCKED */ 1010 -> "Network policy block"
                else -> "Unknown code ($reason)"
            }

            /**
             * Simplified + translatable user facing errors
             */
            @StringRes
            val localizedReason = when (reason) { // @formatter:off
                DownloadManager.ERROR_HTTP_DATA_ERROR,
                DownloadManager.ERROR_TOO_MANY_REDIRECTS,
                DownloadManager.ERROR_UNHANDLED_HTTP_CODE ->
                    R.string.downloader_err_response

                DownloadManager.ERROR_INSUFFICIENT_SPACE ->
                    R.string.downloader_err_storage_space

                DownloadManager.ERROR_FILE_ALREADY_EXISTS ->
                    R.string.downloader_err_file_exists

                else -> R.string.downloader_err_unknown
            } // @formatter:on
        }
    }
}
