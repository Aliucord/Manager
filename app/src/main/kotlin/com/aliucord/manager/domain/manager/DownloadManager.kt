package com.aliucord.manager.domain.manager

import android.app.Application
import android.app.DownloadManager
import android.content.*
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.network.service.GithubService
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DownloadManager(
    private val context: Application
) {
    private val downloadManager = context.getSystemService<DownloadManager>()!!
    private val externalCacheDir = context.externalCacheDir!!

    // Discord APK downloading
    suspend fun downloadDiscordApk(version: String, out: File): File {
        return download("$BACKEND_HOST/download/discord?v=$version", out)
    }

    suspend fun downloadDiscordApk(version: String): File {
        return download("$BACKEND_HOST/download/discord?v=$version", "base-$version.apk")
    }

    suspend fun downloadSplit(version: String, split: String): File {
        return download("$BACKEND_HOST/download/discord?v=$version&split=$split", "$split-$version.apk")
    }

    // Aliucord Kotlin downloads
    suspend fun downloadKtInjector(out: File): File {
        return download(GithubService.KT_INJECTOR_URL, out)
    }

    suspend fun downloadAliuhook(version: String, out: File): File {
        return download(AliucordMavenRepository.getAliuhookUrl(version), out)
    }

    suspend fun downloadKotlinDex(out: File): File {
        return download(GithubService.KOTLIN_DEX_URL, out)
    }

    // Generic downloading
    @Deprecated("use the one below")
    suspend fun download(url: String, fileName: String): File {
        return download(url, externalCacheDir.resolve(fileName))
    }

    suspend fun download(url: String, out: File): File {
        return suspendCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                var downloadId = 0L

                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (downloadId == id) {
                        context.unregisterReceiver(this)
                        continuation.resume(out)
                    }
                }
            }

            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            receiver.downloadId = DownloadManager.Request(url.toUri()).run {
                setTitle("Aliucord Manager: Downloading ${out.name}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationUri(out.toUri())
                downloadManager.enqueue(this)
            }
        }
    }

    companion object {
        private const val BACKEND_HOST = "https://aliucord.com/"
    }
}
