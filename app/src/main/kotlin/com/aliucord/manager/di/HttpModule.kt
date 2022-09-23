package com.aliucord.manager.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Dns
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.net.Inet4Address
import java.net.InetAddress

val httpModule =  module {
    fun provideJson() = Json {
        ignoreUnknownKeys = true
    }

    fun provideHttpClient(json: Json) = HttpClient(OkHttp) {
        engine {
            config {
                dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        // Github's nameservers do not respond to IPv6 requests for raw.githubusercontent.com,
                        // which causes CIO, Android and OkHTTP to all hang
                        return Dns.SYSTEM.lookup(hostname).filterIsInstance<Inet4Address>()
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
}
