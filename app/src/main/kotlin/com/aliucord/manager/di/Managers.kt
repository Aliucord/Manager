package com.aliucord.manager.di

import android.app.Application
import android.content.Context
import com.aliucord.manager.manager.DownloadManager
import com.aliucord.manager.manager.PreferencesManager
import org.koin.core.scope.Scope

fun Scope.providePreferences(): PreferencesManager {
    val ctx: Context = get()
    return PreferencesManager(ctx.getSharedPreferences("preferences", Context.MODE_PRIVATE))
}

fun Scope.provideDownloadManager(): DownloadManager {
    val application: Application = get()
    return DownloadManager(application)
}
