package com.aliucord.manager.manager.download

import com.aliucord.manager.manager.download.IDownloadManager.Result
import com.aliucord.manager.util.IS_PROBABLY_EMULATOR
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * Handle downloading remote urls to a path with Ktor.
 * This is used as an alternative downloader option due to some bugs with the
 * system's DownloadManager that prevents its usage on some emulators and ROMs.
 */
class KtorDownloadManager(private val http: HttpClient) : IDownloadManager {
    override suspend fun download(url: String, out: File, onProgressUpdate: IDownloadManager.ProgressListener?): Result {
        onProgressUpdate?.onUpdate(null)
        out.parentFile?.mkdirs()

        val tmpOut = out.resolveSibling(out.name + ".tmp")
            .apply { exists() || createNewFile() }

        try {
            val httpStmt = http.prepareGet(url) {
                // Disable compression due to bug on emulators
                // This header cannot be set with Android's DownloadManager
                if (IS_PROBABLY_EMULATOR) {
                    header(HttpHeaders.AcceptEncoding, null)
                }
            }

            httpStmt.execute { resp ->
                val channel = resp.bodyAsChannel()
                val total = resp.contentLength() ?: 0
                var retrieved = 0L

                val buf = ByteArray(1024 * 1024 * 1)
                var bufLen = 0

                tmpOut.outputStream().use { stream ->
                    while (!channel.isClosedForRead) {
                        bufLen = channel.readAvailable(buf)
                        if (bufLen <= 0) break

                        stream.write(buf, 0, bufLen)
                        stream.flush()

                        retrieved += bufLen

                        if (total > 0) {
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
    class Error(private val throwable: Throwable) : Result.Error() {
        override fun getDebugReason(): String = throwable.message ?: "Unknown exception"
        override fun toString(): String = throwable.stackTraceToString()
    }
}
