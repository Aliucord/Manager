package com.aliucord.manager.manager.download

import android.app.DownloadManager
import android.content.Context
import java.io.File

/**
 * Common interface for different implementations of starting and managing the lifetime of downloads.
 */
interface IDownloadManager {
    /**
     * Start a cancellable download.
     * @param url Remote src url
     * @param out Target path to download to. It is assumed that the application has write permissions to this path.
     * @param onProgressUpdate An optional [ProgressListener] callback.
     */
    suspend fun download(url: String, out: File, onProgressUpdate: ProgressListener? = null): Result

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
         * This download failed to complete due to an error.
         */
        abstract class Error : Result {
            /**
             * The full internal error representation.
             */
            abstract fun getDebugReason(): String

            /**
             * Simplified + translatable user facing reason for the failure.
             * If null is returned, then the [getDebugReason] will be used instead.
             */
            open fun getLocalizedReason(context: Context): String? = null
        }
    }
}
