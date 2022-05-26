/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.installer.util

import android.app.DownloadManager
import android.content.*
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object DownloadUtils {
    private const val backendHost = "https://aliucord.com/"

    suspend fun downloadDiscordApk(ctx: Context, version: String) =
        download(ctx, "$backendHost/download/discord?v=$version", "base-$version.apk")

    suspend fun downloadSplit(ctx: Context, version: String, split: String) =
        download(ctx, "$backendHost/download/discord?v=$version&split=$split", "$split-$version.apk")

    private suspend fun download(ctx: Context, url: String, fileName: String): File {
        val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        return suspendCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                var downloadId = 0L

                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (downloadId == id) {
                        ctx.unregisterReceiver(this)
                        continuation.resume(context.filesDir)
                    }
                }
            }

            ctx.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            receiver.downloadId = DownloadManager.Request(url.toUri()).run {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                setDestinationUri(ctx.externalCacheDir!!.resolve(fileName).toUri())
                downloadManager.enqueue(this)
            }
        }
    }
}
