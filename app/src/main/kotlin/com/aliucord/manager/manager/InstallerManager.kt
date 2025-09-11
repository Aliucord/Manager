package com.aliucord.manager.manager

import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.pm.PMInstaller
import com.aliucord.manager.installers.shizuku.ShizukuInstaller
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

/**
 * Handle providing the correct install manager based on preferences.
 */
class InstallerManager(
    private val prefs: PreferencesManager,
) : KoinComponent {
    fun getActiveInstaller(): Installer =
        getInstaller(prefs.installer)

    @OptIn(KoinInternalApi::class)
    fun getInstaller(type: InstallerSetting): Installer =
        getKoin().scopeRegistry.rootScope.get(clazz = type.installerClass)
}

enum class InstallerSetting(
    // @StringRes
    // private val localizedName: Int,
    val installerClass: KClass<out Installer>,
) {
    PM(PMInstaller::class),
    Shizuku(ShizukuInstaller::class),
}
