package com.aliucord.manager.di

import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.manager.download.*
import com.aliucord.manager.patcher.util.Signer.getKoin
import com.aliucord.manager.util.IS_PROBABLY_EMULATOR
import org.koin.core.annotation.KoinInternalApi
import kotlin.reflect.KClass

/**
 * Handle providing the correct install manager based on preferences and device type.
 */
class DownloadManagerProvider(private val prefs: PreferencesManager) {
    fun getActiveDownloader(): IDownloadManager =
        getDownloader(prefs.downloader)

    @OptIn(KoinInternalApi::class)
    fun getDownloader(type: DownloaderSetting): IDownloadManager =
        getKoin().scopeRegistry.rootScope.get(clazz = type.downloaderClass)

    companion object {
        fun getDefaultDownloader(): DownloaderSetting {
            // Ktor downloader has specific fix for emulator
            return if (IS_PROBABLY_EMULATOR) {
                DownloaderSetting.Ktor
            } else {
                DownloaderSetting.Android
            }
        }
    }
}

enum class DownloaderSetting(
    // @StringRes
    // private val localizedName: Int,
    val downloaderClass: KClass<out IDownloadManager>,
) {
    Android(AndroidDownloadManager::class),
    Ktor(KtorDownloadManager::class),
}
