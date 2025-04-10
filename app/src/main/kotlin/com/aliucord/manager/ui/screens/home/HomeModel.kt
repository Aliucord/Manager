package com.aliucord.manager.ui.screens.home

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.services.AliucordMavenService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.patcher.InstallMetadata
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.screens.patchopts.PatchOptionsScreen
import com.aliucord.manager.ui.util.DiscordVersion
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import com.github.diamondminer88.zip.ZipReader
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class HomeModel(
    private val application: Application,
    private val github: AliucordGithubService,
    private val maven: AliucordMavenService,
    private val json: Json,
) : ScreenModel {
    var supportedVersion by mutableStateOf<DiscordVersion>(DiscordVersion.None)
        private set

    var state by mutableStateOf<InstallsState>(InstallsState.Fetching)
        private set

    private var remoteDataJson: BuildInfo? = null
    private var latestAliuhookVersion: SemVer? = null

    init {
        // fetchInstallations() is also called from UI first to ensure fast TTI

        screenModelScope.launch {
            fetchRemoteData()
            fetchInstallations() // Re-fetch installations to set the up-to-date statuses
        }
    }

    fun openApp(packageName: String) {
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

    fun fetchInstallations() = screenModelScope.launchBlock {
        state = InstallsState.Fetching

        try {
            val packageManager = application.packageManager

            val installedPackages = packageManager
                .getInstalledPackages(PackageManager.GET_META_DATA)
                .takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Failed to fetch installed packages (returned none)")

            val aliucordPackages = installedPackages
                .asSequence()
                .filter {
                    val isAliucordPkg = it.packageName == "com.aliucord" // Legacy installer builds do not have isAliucord metadata marker
                    val hasAliucordMeta = it.applicationInfo?.metaData?.containsKey("isAliucord") == true
                    isAliucordPkg || hasAliucordMeta
                }

            val aliucordInstallations = aliucordPackages.mapNotNull {
                // `longVersionCode` is unnecessary since Discord doesn't use `versionCodeMajor`
                @Suppress("DEPRECATION")
                val versionCode = it.versionCode
                val versionName = it.versionName ?: return@mapNotNull null
                val applicationInfo = it.applicationInfo ?: return@mapNotNull null

                InstallData(
                    name = packageManager.getApplicationLabel(applicationInfo).toString(),
                    packageName = it.packageName,
                    isUpToDate = isInstallationUpToDate(it),
                    icon = packageManager
                        .getApplicationIcon(applicationInfo)
                        .toBitmap()
                        .asImageBitmap()
                        .let(::BitmapPainter),
                    version = DiscordVersion.Existing(
                        type = DiscordVersion.parseVersionType(versionCode),
                        name = versionName.split("-")[0].trim(),
                        code = versionCode,
                    ),
                )
            }.toImmutableList()

            state = if (aliucordInstallations.isNotEmpty()) {
                InstallsState.Fetched(data = aliucordInstallations)
            } else {
                InstallsState.None
            }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to query Aliucord installations", t)
            state = InstallsState.Error
        }
    }

    /**
     * Creates a [PatchOptionsScreen] that can be navigated to,
     * with prefilled options from an existing installation.
     */
    fun createPrefilledPatchOptsScreen(packageName: String): PatchOptionsScreen {
        val metadata = try {
            val applicationInfo = application.packageManager.getApplicationInfo(packageName, 0)
            val metadataFile = ZipReader(applicationInfo.publicSourceDir)
                .use { it.openEntry("aliucord.json")?.read() }

            @OptIn(ExperimentalSerializationApi::class)
            metadataFile?.let { json.decodeFromStream<InstallMetadata>(it.inputStream()) }
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to parse Aliucord install metadata from package ${packageName}", t)
            null
        }

        val patchOptions = metadata?.options
            ?: PatchOptions.Default.copy(packageName = packageName)

        return PatchOptionsScreen(prefilledOptions = patchOptions)
    }

    suspend fun fetchRemoteData() {
        github.getBuildData().fold(
            success = {
                val versionCode = it.discordVersionCode.toIntOrNull()
                if (versionCode == null) {
                    supportedVersion = DiscordVersion.Error
                    return
                }

                remoteDataJson = it
                supportedVersion = DiscordVersion.Existing(
                    type = DiscordVersion.parseVersionType(versionCode),
                    name = it.discordVersionName.split("-")[0].trim(),
                    code = versionCode,
                )
            },
            fail = {
                Log.w(BuildConfig.TAG, "Failed to fetch remote build data", it)
                supportedVersion = DiscordVersion.Error
            },
        )

        maven.getAliuhookVersion().fold(
            success = { latestAliuhookVersion = it },
            fail = {
                Log.w(BuildConfig.TAG, "Failed to fetch latest Aliuhook version", it)
                supportedVersion = DiscordVersion.Error
            },
        )
    }

    private fun isInstallationUpToDate(pkg: PackageInfo): Boolean {
        // Assume up-to-date when remote data hasn't been fetched yet
        val remoteBuildData = remoteDataJson ?: return true
        val latestAliuhookVersion = latestAliuhookVersion ?: return true

        // `longVersionCode` is unnecessary since Discord doesn't use `versionCodeMajor`
        @Suppress("DEPRECATION")
        val versionCode = pkg.versionCode

        // Check if the base APK version is a mismatch
        if (remoteBuildData.discordVersionCode.toIntOrNull() != versionCode) return false

        // Try to parse install metadata. If none present, install was made via legacy installer.
        val apkPath = pkg.applicationInfo?.publicSourceDir ?: return false
        val installMetadata = try {
            val metadataFile = ZipReader(apkPath).use { it.openEntry("aliucord.json")?.read() }
                ?: return false

            @OptIn(ExperimentalSerializationApi::class)
            json.decodeFromStream<InstallMetadata>(metadataFile.inputStream())
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to parse Aliucord install metadata from package ${pkg.packageName}", t)
            return false
        }

        // Check that all the installation components are up-to-date
        return remoteBuildData.injectorVersion == installMetadata.injectorVersion
            && remoteBuildData.patchesVersion == installMetadata.patchesVersion
            && latestAliuhookVersion == installMetadata.aliuhookVersion
    }
}
