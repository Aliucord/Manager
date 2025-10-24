package com.aliucord.manager.manager.download

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.download.IDownloadManager.Result
import com.aliucord.manager.patcher.util.InsufficientStorageException
import com.aliucord.manager.util.IS_PROBABLY_EMULATOR
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Handle downloading remote urls to a path with Ktor.
 * This is used as an alternative downloader option due to some bugs with the
 * system's DownloadManager that prevents its usage on some emulators and ROMs.
 */
class KtorDownloadManager(
    private val http: HttpClient,
    private val application: Application,
) : IDownloadManager {
    override suspend fun download(url: String, out: File, onProgressUpdate: IDownloadManager.ProgressListener?): Result {
        onProgressUpdate?.onUpdate(null)
        out.parentFile?.mkdirs()

        val tmpOut = out.resolveSibling(out.name + ".tmp")

        try {
            val httpStmt = http.prepareGet(url) {
                header(HttpHeaders.CacheControl, "no-cache, no-store")

                // Disable compression due to bug on emulators
                // This header cannot be set with Android's DownloadManager
                if (IS_PROBABLY_EMULATOR) {
                    header(HttpHeaders.AcceptEncoding, null)
                }
            }

            httpStmt.execute { resp ->
                if (!resp.status.isSuccess()) {
                    val body = try {
                        resp.bodyAsText().take(2048)
                    } catch (e: Exception) {
                        Log.e(BuildConfig.TAG, "Failed to read downloader error response", e)
                        "<failed to read>"
                    }

                    throw DownloadException(url = url, status = resp.status, body = body)
                }

                val channel = resp.bodyAsChannel()
                val total = resp.contentLength() ?: 0
                var retrieved = 0L

                val buf = ByteArray(1024 * 1024 * 1)
                var bufLen: Int

                tmpOut.outputStream().use { stream ->
                    // Preallocate space for this file
                    if (total > 0 && Build.VERSION.SDK_INT >= 26) {
                        val storageManager = application.getSystemService<StorageManager>()!!

                        try {
                            storageManager.allocateBytes(stream.fd, total)
                        } catch (e: IOException) {
                            throw InsufficientStorageException(e.message)
                        }
                    }

                    while (!channel.isClosedForRead) {
                        bufLen = channel.readAvailable(buf)
                        if (bufLen <= 0) break

                        stream.write(buf, 0, bufLen)
                        stream.flush()

                        retrieved += bufLen

                        if (total > 0) {
                            if (retrieved > total)
                                throw IOException("Total bytes received exceeds header total!")

                            onProgressUpdate?.onUpdate(retrieved / total.toFloat())
                        } else {
                            onProgressUpdate?.onUpdate(null)
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            tmpOut.delete()
            return Result.Cancelled(systemTriggered = false)
        } catch (e: DownloadException) {
            tmpOut.delete()
            return Error(
                error = e,
                localizedError = R.string.downloader_err_code,
                localizedErrorArgs = arrayOf(e.status.value),
            )
        } catch (e: SocketTimeoutException) {
            tmpOut.delete()
            return Error(e, localizedError = R.string.downloader_err_timeout)
        } catch (e: InsufficientStorageException) {
            tmpOut.delete()
            return Error(e, localizedError = R.string.downloader_err_storage_space)
        } catch (t: Throwable) {
            tmpOut.delete()
            return Error(t)
        }

        tmpOut.renameTo(out)
        return Result.Success(out)
    }

    /**
     * Wrapper around an exception that occurred from invoking Ktor
     */
    class Error(
        private val error: Throwable,
        @StringRes
        private val localizedError: Int? = null,
        private val localizedErrorArgs: Array<Any> = arrayOf(),
    ) : Result.Error() {
        override fun toString(): String = error.stackTraceToString()
        override fun getDebugReason(): String = error.message ?: "Unknown exception"
        override fun getLocalizedReason(context: Context): String? =
            localizedError?.let { context.getString(it, *localizedErrorArgs) }

        override fun getError(): Throwable? = error
    }

    private class DownloadException(val url: String, val status: HttpStatusCode, val body: String) :
        IOException("Failed to download $url, received status code $status, response: $body")
}
