package com.aliucord.manager

import android.app.Application
import com.aliucord.manager.di.*
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.service.*
import com.aliucord.manager.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ManagerApplication)

            // HTTP
            modules(module {
                single { provideJson() }
                single { provideHttpClient() }
            })

            // Services
            modules(module {
                singleOf(::HttpService)
                singleOf(::GithubService)
                singleOf(::MavenService)
                singleOf(::AliucordGithubService)
            })

            // Repositories
            modules(module {
                singleOf(::GithubRepository)
                singleOf(::AliucordMavenRepository)
            })

            // ViewModels
            modules(module {
                viewModelOf(::HomeViewModel)
                viewModelOf(::PluginsViewModel)
                viewModelOf(::AboutViewModel)
                viewModelOf(::InstallViewModel)
                viewModelOf(::SettingsViewModel)
                viewModelOf(::UpdaterViewModel)
            })

            // Managers
            modules(module {
                single { providePreferences() }
                single { provideDownloadManager() }
            })
        }
    }
}
