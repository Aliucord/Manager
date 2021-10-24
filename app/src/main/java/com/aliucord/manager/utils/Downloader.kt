package com.aliucord.manager.utils

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import java.io.File

object Downloader {

    suspend fun downloadFile(
        url: String,
        filePath: String,
        onSuccess: () -> Unit,
        onFailure: (statusCode: Int) -> Unit,
        onProgress: (progress: Long) -> Unit
    ) {
        val file = File(filePath)
        Http.ktorClient.get<HttpStatement>(url) {
            onDownload { bytesSentTotal, contentLength ->
                onProgress(bytesSentTotal / contentLength)
            }
        }.execute {
            if (!it.status.isSuccess()) {
                onFailure(it.status.value)
                return@execute
            }
            file.appendBytes(it.readBytes())
        }
        onSuccess()
    }

}