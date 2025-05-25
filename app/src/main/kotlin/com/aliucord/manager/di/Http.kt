package com.aliucord.manager.di

import android.app.Application
import com.aliucord.manager.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.Json
import okhttp3.Dns
import org.koin.core.scope.Scope
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext

@Suppress("UnusedReceiverParameter")
fun Scope.provideJson() = Json {
    ignoreUnknownKeys = true
}

fun Scope.provideHttpClient() = HttpClient(OkHttp) {
    val json: Json = get()
    val application: Application = get()

    defaultRequest {
        header(HttpHeaders.UserAgent, "Aliucord Manager/${BuildConfig.VERSION_NAME}")
    }

    engine {
        config {
            dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val addresses = Dns.SYSTEM.lookup(hostname)

                    // Github's nameservers do not respond to IPv6 requests for raw.githubusercontent.com,
                    // which causes CIO, Android and OkHTTP to all hang
                    return if (hostname == "raw.githubusercontent.com") {
                        addresses.filterIsInstance<Inet4Address>()
                    } else {
                        addresses
                    }
                }
            })
        }
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 20000
        requestTimeoutMillis = 10000
    }

    install(HttpCache) {
        val dir = application.cacheDir.resolve("ktor")
        publicStorage(FileStorage(dir))
    }

    install(HttpCookies) {
        // Default storage is in-memory
    }

    // Custom plugin to allow overriding response cache headers, and force caching
    install("OverrideCacheControl") {
        receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
            val customCacheControl = response.call.attributes.getOrNull(CustomCacheControl)
                ?: return@intercept

            proceedWith(object : HttpResponse() {
                @InternalAPI
                override val rawContent: ByteReadChannel = response.rawContent
                override val call: HttpClientCall = response.call
                override val coroutineContext: CoroutineContext = response.coroutineContext
                override val requestTime: GMTDate = response.requestTime
                override val responseTime: GMTDate = response.responseTime
                override val status: HttpStatusCode = response.status
                override val version: HttpProtocolVersion = response.version

                override val headers: Headers = headers {
                    appendAll(response.headers)
                    set(HttpHeaders.CacheControl, customCacheControl.toString())
                }
            })
        }
    }
}

private val CustomCacheControl: AttributeKey<CacheControl> = AttributeKey("CustomCacheControl")

fun HttpRequestBuilder.cacheControl(cacheControl: CacheControl) {
    attributes.put(CustomCacheControl, cacheControl)
}
