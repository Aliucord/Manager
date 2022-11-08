package com.aliucord.manager.di

import com.aliucord.manager.installer.util.IconPatcher
import com.aliucord.manager.network.service.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::GithubService)
    singleOf(::MavenService)
    singleOf(::AliucordGithubService)
    singleOf(::IconPatcher)
}
