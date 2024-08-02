package com.aliucord.manager.manager.download

import com.aliucord.manager.manager.download.IDownloadManager.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.min

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
                header(HttpHeaders.AcceptEncoding, null)
            }

            httpStmt.execute { resp ->
                val channel = resp.bodyAsChannel()
                val total = resp.contentLength() ?: 0
                var retrieved = 0L

                tmpOut.outputStream().use { stream ->
                    while (!channel.isClosedForRead) {
                        channel.awaitContent()
                        val packet = channel.readRemaining(1024 * 1024 * 1)

                        while (!packet.isEmpty) {
                            // TODO: reuse bytearray
                            val bytes = packet.readBytes()
                            stream.write(bytes)
                            stream.flush()

                            retrieved += bytes.size

                            if (total > 0) {
                                onProgressUpdate?.onUpdate(retrieved / total.toFloat())
                            } else {
                                onProgressUpdate?.onUpdate(null)
                            }
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
