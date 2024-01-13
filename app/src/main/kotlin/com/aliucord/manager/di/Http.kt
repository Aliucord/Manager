package com.aliucord.manager.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Dns
import org.koin.core.scope.Scope
import java.net.Inet4Address
import java.net.InetAddress

@Suppress("UnusedReceiverParameter")
fun Scope.provideJson() = Json {
    ignoreUnknownKeys = true
}

fun Scope.provideHttpClient() = HttpClient(OkHttp) {
    val json: Json = get()

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
}
