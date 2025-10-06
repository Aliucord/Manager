package com.aliucord.manager.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.intent.IntentInstaller
import com.aliucord.manager.installers.pm.PMInstaller
import com.aliucord.manager.installers.root.RootInstaller
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

enum class InstallerSetting(val installerClass: KClass<out Installer>) {
    PM(PMInstaller::class),
    Root(RootInstaller::class),
    Intent(IntentInstaller::class),
    Shizuku(ShizukuInstaller::class);

    @Composable
    fun title() = when (this) {
        PM -> stringResource(R.string.installer_pm)
        Root -> stringResource(R.string.installer_root)
        Intent -> stringResource(R.string.installer_intent)
        Shizuku -> stringResource(R.string.installer_shizuku)
    }

    @Composable
    fun description() = when (this) {
        PM -> stringResource(R.string.installer_pm_desc)
        Root -> stringResource(R.string.installer_root_desc)
        Intent -> stringResource(R.string.installer_intent_desc)
        Shizuku -> stringResource(R.string.installer_shizuku_desc)
    }

    @Composable
    fun icon() = when (this) {
        PM -> painterResource(R.drawable.ic_android)
        Root -> painterResource(R.drawable.ic_hashtag)
        Intent -> painterResource(R.drawable.ic_launch)
        Shizuku -> painterResource(R.drawable.ic_shizuku)
    }
}
