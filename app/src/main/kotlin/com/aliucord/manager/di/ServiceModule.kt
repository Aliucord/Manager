package com.aliucord.manager.di

import com.aliucord.manager.network.service.GithubService
import com.aliucord.manager.network.service.MavenService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::GithubService)
    singleOf(::MavenService)
}
