package com.aliucord.manager

import android.app.Application
import com.aliucord.manager.di.*
import com.aliucord.manager.domain.repository.AliucordMavenRepository
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installers.pm.PMInstaller
import com.aliucord.manager.manager.*
import com.aliucord.manager.manager.download.AndroidDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.network.service.*
import com.aliucord.manager.ui.screens.about.AboutModel
import com.aliucord.manager.ui.screens.home.HomeModel
import com.aliucord.manager.ui.screens.patching.PatchingScreenModel
import com.aliucord.manager.ui.screens.patchopts.PatchOptionsModel
import com.aliucord.manager.ui.screens.plugins.PluginsModel
import com.aliucord.manager.ui.screens.settings.SettingsModel
import com.aliucord.manager.ui.widgets.updater.UpdaterViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.*
import org.koin.dsl.module

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Android activities & context
            androidContext(this@ManagerApplication)
            modules(module(createdAtStart = true) {
                singleOf(::ActivityProvider)
            })

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

            // UI Models
            modules(module {
                factoryOf(::HomeModel)
                factoryOf(::PluginsModel)
                factoryOf(::AboutModel)
                factoryOf(::PatchingScreenModel)
                factoryOf(::SettingsModel)
                factoryOf(::PatchOptionsModel)
                viewModelOf(::UpdaterViewModel)
            })

            // Managers
            modules(module {
                single { providePreferences() }
                singleOf(::PathManager)
                singleOf(::InstallerManager)
                singleOf(::OverlayManager)

                singleOf(::DownloadManagerProvider)
                singleOf(::AndroidDownloadManager)
                singleOf(::KtorDownloadManager)
            })

            // Installers
            modules(module {
                singleOf(::PMInstaller)
            })
        }
    }
}
