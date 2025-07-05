package com.aliucord.manager.ui.widgets.updater

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.manager.InstallerManager
import com.aliucord.manager.manager.InstallerSetting
import com.aliucord.manager.manager.download.IDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class UpdaterViewModel(
    private val github: AliucordGithubService,
    private val downloader: KtorDownloadManager,
    private val installers: InstallerManager,
    private val application: Application,
) : ViewModel() {
    var showDialog by mutableStateOf(false)
        private set
    var targetVersion by mutableStateOf<String?>(null)
        private set
    var isWorking by mutableStateOf(false)
        private set

    private var targetApkUrl: String? = null

    init {
        viewModelScope.launch {
            try {
                fetchInfo()
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to check for updates!", t)
                application.showToast(R.string.updater_check_fail)
            }
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun triggerUpdate() {
        if (isWorking) return
        val url = targetApkUrl ?: return

        viewModelScope.launch {
            isWorking = true

            val apkFile = application.cacheDir.resolve("manager.apk")

            try {
                apkFile.apply {
                    parentFile!!.mkdirs()
                    exists() && delete()
                }

                val installer = installers.getInstaller(InstallerSetting.PM)

                when (val downloadResult = downloader.download(url, apkFile)) {
                    is IDownloadManager.Result.Success ->
                        Log.d(BuildConfig.TAG, "Downloaded update")

                    is IDownloadManager.Result.Cancelled -> {
                        Log.i(BuildConfig.TAG, "Update cancelled")
                        return@launch
                    }

                    is IDownloadManager.Result.Error ->
                        throw IllegalStateException("Failed to download update: ${downloadResult.getDebugReason()}", downloadResult.getError())
                }

                val installResult = installer.waitInstall(
                    apks = listOf(apkFile),
                    silent = true,
                )

                when (installResult) {
                    InstallerResult.Success -> {
                        Log.w(BuildConfig.TAG, "Update completed without restarting app!")
                        exitProcess(1)
                    }

                    is InstallerResult.Cancelled ->
                        Log.i(BuildConfig.TAG, "Update cancelled")

                    is InstallerResult.Error ->
                        throw Exception("Failed to install update: ${installResult.getDebugReason()}")
                }
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to perform update!")
                t.printStackTrace()
                application.showToast(R.string.updater_update_fail)
                launchReleasesPage()
            } finally {
                isWorking = false

                try {
                    apkFile.apply { exists() && delete() }
                } catch (t: Throwable) {
                    Log.w(BuildConfig.TAG, "Failed to clean up installed update!", t)
                }
            }
        }
    }

    /**
     * This fetched the releases data from GitHub and populates the state if there is an update.
     *
     * It obtains all the releases that have an asset named `aliucord-manager-[tag].apk`,
     * then finds the latest release based on the largest semantic version extracted from the tag name (`v1.0.0`),
     * and populates the state to show to the user.
     */
    private suspend fun fetchInfo() {
        Log.d(BuildConfig.TAG, "Checking for updates...")

        val currentVersion = SemVer.parseOrNull(BuildConfig.VERSION_NAME)
            ?: throw Error("Failed to parse current app version")

        // Fetch releases from GitHub (60s local cache)
        val releases = github.getManagerReleases().getOrThrow()

        // Find the latest release + APK release asset
        val (version, apkUrl) = releases
            .mapNotNull { release ->
                val version = SemVer.parseOrNull(release.tagName)
                    ?: return@mapNotNull null

                val asset = release.assets.find { it.name == "aliucord-manager-${release.tagName}.apk" }
                    ?: return@mapNotNull null

                version to asset.browserDownloadUrl
            }
            .maxByOrNull { it.first }
            ?: return

        // Check if currently installed version is greater
        if (currentVersion >= version) {
            Log.d(BuildConfig.TAG, "Already updated to latest version!")
            return
        }

        targetVersion = version.toString()
        targetApkUrl = apkUrl
        showDialog = true
        Log.d(BuildConfig.TAG, "Found an update! $targetVersion $targetApkUrl")
    }

    private fun launchReleasesPage() {
        try {
            Intent(Intent.ACTION_VIEW, AliucordGithubService.LATEST_RELEASE_HTML_URL.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .also(application::startActivity)
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to open latest Github release in browser!", t)
        }
    }
}
