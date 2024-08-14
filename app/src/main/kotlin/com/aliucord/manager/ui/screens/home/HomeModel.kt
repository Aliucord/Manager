package com.aliucord.manager.ui.screens.home

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import kotlinx.collections.immutable.toImmutableList

class HomeModel(
    private val application: Application,
    private val github: GithubRepository,
) : ScreenModel {
    var supportedVersion by mutableStateOf<DiscordVersion>(DiscordVersion.None)
        private set

    var installations by mutableStateOf<InstallsState>(InstallsState.Fetching)
        private set

    init {
        // fetchInstallations() is called from UI
        fetchSupportedVersion()
    }

    fun launchApp(packageName: String) {
        val launchIntent = application.packageManager
            .getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            application.startActivity(launchIntent)
        } else {
            application.showToast(R.string.launch_aliucord_fail)
        }
    }

    fun openAppInfo(packageName: String) {
        val launchIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setData("package:$packageName".toUri())

        application.startActivity(launchIntent)
    }

    /**
     * Check whether the device is connected on a metered WIFI connection or through any type of mobile data,
     * to avoid unknowingly downloading a lot of stuff through a potentially metered network.
     */
    @Suppress("DEPRECATION")
    fun isNetworkDangerous(): Boolean {
        val connectivity = application.getSystemService<ConnectivityManager>()
            ?: error("Unable to get system connectivity service")

        if (connectivity.isActiveNetworkMetered) return true

        when (val info = connectivity.activeNetworkInfo) {
            null -> return false
            else -> {
                if (info.isRoaming) return true
                if (info.type == ConnectivityManager.TYPE_WIFI) return false
            }
        }

        val telephony = application.getSystemService<TelephonyManager>()
            ?: error("Unable to get system telephony service")

        val dangerousMobileDataStates = arrayOf(
            /* TelephonyManager.DATA_DISCONNECTING */ 4,
            TelephonyManager.DATA_CONNECTED,
            TelephonyManager.DATA_CONNECTING,
        )

        return dangerousMobileDataStates.contains(telephony.dataState)
    }

    fun fetchInstallations() = screenModelScope.launchBlock {
        installations = InstallsState.Fetching

        try {
            val packageManager = application.packageManager

            val installedPackages = packageManager
                .getInstalledPackages(PackageManager.GET_META_DATA)
                .takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Failed to fetch installed packages (returned none)")

            val aliucordPackages = installedPackages
                .asSequence()
                .filter {
                    val isAliucordPkg = it.packageName == "com.aliucord"
                    val hasAliucordMeta = it.applicationInfo.metaData?.containsKey("isAliucord") == true
                    isAliucordPkg || hasAliucordMeta
                }

            val aliucordInstallations = aliucordPackages.map {
                // `longVersionCode` is unnecessary since Discord doesn't use `versionCodeMajor`
                @Suppress("DEPRECATION")
                val versionCode = it.versionCode

                val baseVersion = it.applicationInfo.metaData?.getInt("aliucordBaseVersion")
                val isBaseUpdated = /* TODO: remote data json instead */ baseVersion == 0

                InstallData(
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
            }.toImmutableList()

            installations = if (aliucordInstallations.isNotEmpty()) {
                InstallsState.Fetched(data = aliucordInstallations)
            } else {
                InstallsState.None
            }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to query Aliucord installations", t)
            installations = InstallsState.Error
        }
    }

    private fun fetchSupportedVersion() = screenModelScope.launchBlock {
        github.getDataJson().fold(
            success = {
                val versionCode = it.discordVersionCode.toIntOrNull() ?: return@fold

                supportedVersion = DiscordVersion.Existing(
                    type = DiscordVersion.parseVersionType(versionCode),
                    name = it.discordVersionName.split("-")[0].trim(),
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
