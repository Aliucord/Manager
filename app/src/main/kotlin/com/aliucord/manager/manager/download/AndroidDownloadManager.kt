package com.aliucord.manager.manager.download

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.download.IDownloadManager.ProgressListener
import com.aliucord.manager.manager.download.IDownloadManager.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handle downloading remote urls to a path through the system's [DownloadManager].
 */
class AndroidDownloadManager(application: Application) : IDownloadManager {
    private val downloadManager = application.getSystemService<DownloadManager>()
        ?: throw IllegalStateException("DownloadManager service is not available")

    /**
     * Start a cancellable download with the system [IDownloadManager].
     * If the current [CoroutineScope] is cancelled, then the system download will be cancelled within 100ms.
     * @param url Remote src url
     * @param out Target path to download to. It is assumed that the application has write permissions to this path.
     * @param onProgressUpdate An optional [ProgressListener]
     */
    override suspend fun download(url: String, out: File, onProgressUpdate: ProgressListener?): Result {
        onProgressUpdate?.onUpdate(null)
        out.parentFile?.mkdirs()

        // Create and start a download in the system DownloadManager
        val downloadId = DownloadManager.Request(url.toUri())
            .setTitle("Aliucord Manager")
            .setDescription("Downloading ${out.name}...")
            .setDestinationUri(Uri.fromFile(out))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .addRequestHeader("User-Agent", "Aliucord Manager/${BuildConfig.VERSION_NAME}")
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

                        return Error(reason)
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
     * Error returned by the system [DownloadManager].
     * @param reason The reason code returned by the [DownloadManager.COLUMN_REASON] column.
     */
    data class Error(val reason: Int) : Result.Error() {
        /**
         * Convert a [DownloadManager.COLUMN_REASON] code into its name.
         */
        override fun getDebugReason(): String = when (reason) {
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

        override fun getLocalizedReason(context: Context): String {
            val string = when (reason) { // @formatter:off
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

            return context.getString(string)
        }

        override fun toString(): String = getDebugReason()
    }
}
