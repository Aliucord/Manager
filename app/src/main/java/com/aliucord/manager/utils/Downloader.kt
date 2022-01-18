package com.aliucord.manager.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import java.io.File
import java.util.concurrent.CountDownLatch

object Downloader {
    fun download(ctx: Context, url: String, file: File) {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val latch = CountDownLatch(1)
        with (DownloadReceiver(ctx, latch)) {
            ctx.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            downloadId = DownloadManager.Request(Uri.parse(url)).run {
                setTitle("AliucordManager")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                setDestinationUri(Uri.fromFile(file))
                dm.enqueue(this)
            }
        }
        latch.await()
    }
}

class DownloadReceiver(private val ctx: Context, private val latch: CountDownLatch) : BroadcastReceiver() {
    var downloadId = 0L

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == id) {
            latch.countDown()
            ctx.unregisterReceiver(this)
        }
    }
}
