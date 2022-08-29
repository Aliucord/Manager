package com.aliucord.manager

import android.app.Application
import com.aliucord.manager.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ManagerApplication)
            modules(httpModule, serviceModule, repositoryModule, viewModelModule, managerModule)
        }
    }
}
