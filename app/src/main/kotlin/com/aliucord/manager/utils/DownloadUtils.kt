package com.aliucord.manager.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object DownloadUtils {
    private const val org = "Aliucord"
    private const val repo = "Aliucord"

    private const val backendHost = "https://aliucord.com/"
    private const val contentUrl = "https://raw.githubusercontent.com/${org}/${repo}"

    suspend fun downloadDiscordApk(ctx: Context, version: String) =
        download(ctx, "$backendHost/download/discord?v=$version", "discord-$version.apk")

    suspend fun downloadManifest(ctx: Context, useDebuggableManifest: Boolean) {
        if (useDebuggableManifest)
            download(ctx, "$contentUrl/main/.assets/AndroidManifest-debuggable.xml", "AndroidManifest-debuggable.xml")
        else {
            download(ctx, "$contentUrl/main/.assets/AndroidManifest.xml", "AndroidManifest.xml")
        }
    }

    suspend fun downloadInjector(ctx: Context) = download(ctx, "$contentUrl/builds/Injector.dex", "Injector.dex")

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
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationUri(File(ctx.externalCacheDir, fileName).toUri())
                downloadManager.enqueue(this)
            }
        }
    }
}