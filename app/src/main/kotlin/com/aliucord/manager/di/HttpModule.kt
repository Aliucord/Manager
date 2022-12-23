package com.aliucord.manager.di

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.network.service.HttpService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Dns
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.net.Inet4Address
import java.net.InetAddress

val httpModule = module {
    fun provideJson() = Json {
        ignoreUnknownKeys = true
    }

    fun provideHttpClient(json: Json, preferences: PreferencesManager) = HttpClient(OkHttp) {
        defaultRequest {
            header(HttpHeaders.UserAgent, "Aliucord Manager/${BuildConfig.VERSION_NAME}")
        }
        engine {
            config {
                followSslRedirects(!preferences.httpOnly)
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

    singleOf(::provideJson)
    singleOf(::provideHttpClient)
    singleOf(::HttpService)
}
