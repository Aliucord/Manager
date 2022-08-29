package com.aliucord.manager.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val httpModule =  module {
    fun provideJson() = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun provideHttpClient(json: Json) = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    singleOf(::provideJson)
    singleOf(::provideHttpClient)
}
