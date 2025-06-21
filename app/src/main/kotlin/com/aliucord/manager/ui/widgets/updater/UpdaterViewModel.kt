package com.aliucord.manager.ui.widgets.updater

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.InstallerManager
import com.aliucord.manager.manager.InstallerSetting
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.launch

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
                application.showToast(R.string.updater_fail)
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

            val apkFile = application.cacheDir.resolve("manager.apk").apply { delete() }

            downloader.download(url, apkFile)
            installers.getInstaller(InstallerSetting.PM).install(
                apks = listOf(apkFile),
                silent = true,
            )

            apkFile.delete()
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
}
