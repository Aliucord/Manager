package com.aliucord.manager.domain.manager

import android.app.DownloadManager
import android.content.*
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DownloadManager(
    private val context: Context
) {
    private val downloadManager = context.getSystemService<DownloadManager>()!!

    suspend fun downloadDiscordApk(version: String): File {
        return download("$BACKEND_HOST/download/discord?v=$version", "base-$version.apk")
    }

    suspend fun downloadSplit(version: String, split: String): File {
        return download("$BACKEND_HOST/download/discord?v=$version&split=$split", "$split-$version.apk")
    }

    suspend fun download(url: String, fileName: String): File {
        return suspendCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                var downloadId = 0L

                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (downloadId == id) {
                        context.unregisterReceiver(this)
                        continuation.resume(context.externalCacheDir!!.resolve(fileName))
                    }
                }
            }

            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            receiver.downloadId = DownloadManager.Request(url.toUri()).run {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                setDestinationUri(context.externalCacheDir!!.resolve(fileName).toUri())
                downloadManager.enqueue(this)
            }
        }
    }

    companion object {
        private const val BACKEND_HOST = "https://aliucord.com/"
    }
}
