package com.aliucord.manager.domain.manager

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.network.service.AliucordGithubService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class DownloadManager(
    private val preferences: PreferencesManager,
    private val aliucordMaven: AliucordMavenRepository,
    private val aliucordGithub: AliucordGithubService,
) {
    private val httpClient = HttpClient(OkHttp) {
        followRedirects = !preferences.httpOnly
        engine {
            config {
                followRedirects(!preferences.httpOnly)
                followSslRedirects(!preferences.httpOnly)
            }
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "Aliucord Manager/${BuildConfig.VERSION_NAME}")
        }
    }

    private fun getBaseUrl() = if (preferences.httpOnly) {
        BuildConfig.BACKEND_URL.replace("https", "http")
    } else {
        BuildConfig.BACKEND_URL
    }

    // Discord APK downloading
    suspend fun downloadDiscordApk(version: String, out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download("${getBaseUrl()}/download/discord?v=$version", out, progress)

    suspend fun downloadSplit(version: String, split: String, out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download("${getBaseUrl()}/download/discord?v=$version&split=$split", out, progress)

    // Aliucord Kotlin downloads
    suspend fun downloadKtInjector(out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download(aliucordGithub.getKtInjectorUrl(), out, progress)

    suspend fun downloadAliuhook(version: String, out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download(aliucordMaven.getAliuhookUrl(version), out, progress)

    suspend fun downloadKotlinDex(out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download(aliucordGithub.getKtDexUrl(), out, progress)

    suspend fun downloadBootstrap(out: File, progress: MutableSharedFlow<Int?>? = null): File =
        download(aliucordGithub.getBootstrapUrl(), out, progress)

    /**
     * Download to a file with Ktor while keeping progress
     * @param progress A flow that emits download progress (percentage or null when unknown)
     */
    suspend fun download(url: String, out: File, progress: MutableSharedFlow<Int?>? = null): File {
        withContext(Dispatchers.IO) {
            out.parentFile?.mkdirs()
            val tmpOut = out.resolveSibling("${out.name}.tmp")
                .apply { exists() || createNewFile() }

            val newUrl = if (!preferences.httpOnly) url else run {
                var redirect = url

                for (i in 0 until 5) {
                    val resp = httpClient.head {
                        url(url)
                        method = HttpMethod.Head
                    }

                    val header = resp.headers[HttpHeaders.Location]
                    val replaced = header?.replace("https", "http")
                    println("$redirect $header")
                    if (header == null) {
                        return@run redirect
                    } else if (redirect == replaced) {
                        throw Error("Failed to download in HTTP only mode, encountered unavoidable redirect to HTTPS")
                    } else {
                        redirect = replaced!!
                    }
                }

                throw Error("Reached max limit of redirects while downloading")
            }

            httpClient.prepareGet(newUrl).execute {
                val channel = it.bodyAsChannel()
                val total = it.contentLength() ?: 0
                var retrieved = 0L

                tmpOut.outputStream().use { out ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(1024 * 1024 * 1)
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            withContext(Dispatchers.IO) {
                                out.write(bytes)
                                out.flush()
                            }

                            if (progress != null) {
                                if (total > 0) {
                                    retrieved += bytes.size
                                    progress.emit(min(100, (retrieved * 100 / total).toInt()))
                                } else {
                                    progress.emit(null)
                                }
                            }
                        }
                        channel.awaitContent()
                    }
                }
            }

            tmpOut.renameTo(out)
        }

        return out
    }
}
