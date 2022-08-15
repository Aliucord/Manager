package com.aliucord.manager.di

import com.aliucord.manager.network.service.GithubService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::GithubService)
}
