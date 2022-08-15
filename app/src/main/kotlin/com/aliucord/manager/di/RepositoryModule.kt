package com.aliucord.manager.di

import com.aliucord.manager.domain.repository.GithubRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::GithubRepository)
}
