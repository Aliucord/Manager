package com.aliucord.manager.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object DownloadUtils {
    private const val backendHost = "https://aliucord.com/"

    suspend fun downloadDiscordApk(ctx: Context, version: String) = download(ctx, "$backendHost/download/discord?v=$version")

    suspend fun download(ctx: Context, url: String): File {
        val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        return suspendCoroutine { continuation ->
            try {
                val receiver = object : BroadcastReceiver() {
                    var downloadId = 0L

                    override fun onReceive(context: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                        if (downloadId == id) {
                            Toast.makeText(ctx, "Finished downloading", Toast.LENGTH_SHORT).show()
                            ctx.unregisterReceiver(this)
                            continuation.resume(downloadManager.getUriForDownloadedFile(id).toFile())
                        }
                    }
                }

                ctx.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

                receiver.downloadId = DownloadManager.Request(url.toUri()).run {
                    setTitle("Downloading APK")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    downloadManager.enqueue(this)
                }
            } finally {
                Toast.makeText(ctx, "Finished downloading", Toast.LENGTH_SHORT).show()
            }
        }
    }
}