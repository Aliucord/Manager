package com.aliucord.manager.ui.screens.home

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.uninstallApk
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import kotlinx.collections.immutable.toImmutableList

class HomeModel(
    private val application: Application,
    private val github: GithubRepository,
    private val preferences: PreferencesManager,
) : ScreenModel {
    var supportedVersion by mutableStateOf<DiscordVersion>(DiscordVersion.None)
        private set

    var installations by mutableStateOf<InstallsFetchState>(InstallsFetchState.Fetching)
        private set

    init {
        refresh()
    }

    fun refresh() {
        fetchInstallations()
        fetchSupportedVersion()
    }

    fun launchAliucord() {
        val launchIntent = application.packageManager
            .getLaunchIntentForPackage(preferences.packageName)

        if (launchIntent != null) {
            application.startActivity(launchIntent)
        } else {
            application.showToast(R.string.launch_aliucord_fail)
        }
    }

    fun uninstallAliucord() {
        application.uninstallApk(preferences.packageName)
    }

    private fun fetchInstallations() = screenModelScope.launchBlock {
        try {
            val packageManager = application.packageManager

            val installedPackages = packageManager
                .getInstalledPackages(PackageManager.GET_META_DATA)
                .takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Failed to fetch installed packages (returned none)")

            println(installedPackages)

            val aliucordPackages = installedPackages
                .asSequence()
                .filter {
                    val isAliucordPkg = it.packageName == "com.aliucord"
                    val hasAliucordMeta = it.applicationInfo.metaData?.containsKey("isAliucord") == true
                    isAliucordPkg || hasAliucordMeta
                }

            println(aliucordPackages)
            val aliucordInstallations = aliucordPackages
                .map {
                    // `longVersionCode` is unnecessary since Discord doesn't use `versionCodeMajor`
                    @Suppress("DEPRECATION")
                    val versionCode = it.versionCode

                    val baseVersion = it.applicationInfo.metaData?.getInt("aliucordBaseVersion")
                    val isBaseUpdated = /* TODO: remote data json instead */ baseVersion == 0

                    InstallsFetchState.Data(
                        name = packageManager.getApplicationLabel(it.applicationInfo).toString(),
                        packageName = it.packageName,
                        baseUpdated = isBaseUpdated,
                        icon = packageManager
                            .getApplicationIcon(it.applicationInfo)
                            .toBitmap()
                            .asImageBitmap()
                            .let(::BitmapPainter),
                        version = DiscordVersion.Existing(
                            type = DiscordVersion.parseVersionType(versionCode),
                            name = it.versionName.split("-")[0].trim(),
                            code = versionCode,
                        ),
                    )
                }

            installations = InstallsFetchState.Fetched(data = aliucordInstallations.toImmutableList())
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to query Aliucord installations", t)
            installations = InstallsFetchState.Error
        }
    }

    private fun fetchSupportedVersion() = screenModelScope.launchBlock {
        github.getDataJson().fold(
            success = {
                val versionCode = it.versionCode.toIntOrNull() ?: return@fold

                supportedVersion = DiscordVersion.Existing(
                    type = DiscordVersion.parseVersionType(versionCode),
                    name = it.versionName.split("-")[0].trim(),
                    code = versionCode,
                )
            },
            fail = {
                Log.e(BuildConfig.TAG, Log.getStackTraceString(it))
                supportedVersion = DiscordVersion.Error
            }
        )
    }
}
