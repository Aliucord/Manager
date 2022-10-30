package com.aliucord.manager.di

import android.app.Application
import android.content.Context
import com.aliucord.manager.domain.manager.DownloadManager
import com.aliucord.manager.domain.manager.PreferencesManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    fun providePreferences(context: Context): PreferencesManager {
        return PreferencesManager(context.getSharedPreferences("preferences", Context.MODE_PRIVATE))
    }

    fun provideDownloadManager(application: Application) = DownloadManager(application)

    singleOf(::providePreferences)
    singleOf(::provideDownloadManager)
}
