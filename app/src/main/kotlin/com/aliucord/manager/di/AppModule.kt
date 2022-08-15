package com.aliucord.manager.di

import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    singleOf(::provideJson)
}
