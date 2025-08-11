package com.aliucord.manager

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.aliucord.manager.di.*
import com.aliucord.manager.installers.pm.PMInstaller
import com.aliucord.manager.manager.*
import com.aliucord.manager.manager.download.AndroidDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.network.services.*
import com.aliucord.manager.ui.screens.about.AboutModel
import com.aliucord.manager.ui.screens.home.HomeModel
import com.aliucord.manager.ui.screens.iconopts.IconOptionsModel
import com.aliucord.manager.ui.screens.log.LogScreenModel
import com.aliucord.manager.ui.screens.logs.LogsListScreenModel
import com.aliucord.manager.ui.screens.patching.PatchingScreenModel
import com.aliucord.manager.ui.screens.patchopts.PatchOptionsModel
import com.aliucord.manager.ui.screens.permissions.PermissionsModel
import com.aliucord.manager.ui.screens.plugins.PluginsModel
import com.aliucord.manager.ui.screens.settings.SettingsModel
import com.aliucord.manager.ui.widgets.updater.UpdaterViewModel
import kotlinx.coroutines.Dispatchers
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
                singleOf(::AliucordGithubService)
                singleOf(::AliucordMavenService)
            })

            // UI Models
            modules(module {
                factoryOf(::HomeModel)
                factoryOf(::PluginsModel)
                factoryOf(::AboutModel)
                factoryOf(::PatchingScreenModel)
                factoryOf(::SettingsModel)
                factoryOf(::PatchOptionsModel)
                factoryOf(::IconOptionsModel)
                factoryOf(::LogScreenModel)
                factoryOf(::LogsListScreenModel)
                factoryOf(::PermissionsModel)
                viewModelOf(::UpdaterViewModel)
            })

            // Managers
            modules(module {
                single { providePreferences() }
                singleOf(::PathManager)
                singleOf(::InstallerManager)
                singleOf(::OverlayManager)
                singleOf(::InstallLogManager)

                singleOf(::AndroidDownloadManager)
                singleOf(::KtorDownloadManager)
            })

            // Installers
            modules(module {
                singleOf(::PMInstaller)
            })
        }

        // Limit parallel fetching of images using Coil
        @OptIn(DelicateCoilApi::class)
        SingletonImageLoader.setUnsafe { context ->
            ImageLoader.Builder(context)
                .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(5))
                .build()
        }
    }
}
