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
import com.aliucord.manager.manager.download.IDownloadManager
import com.aliucord.manager.manager.download.KtorDownloadManager
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.system.exitProcess

class UpdaterViewModel(
    private val github: AliucordGithubService,
    private val downloader: KtorDownloadManager,
    private val installers: InstallerManager,
    private val application: Application,
) : ViewModel() {
    var showDialog by mutableStateOf(false)
        private set
    var targetReleaseUrl by mutableStateOf<String?>(null)
        private set
    var targetVersion by mutableStateOf<String?>(null)
        private set
    val downloadProgress: StateFlow<Float?>
        private field = MutableStateFlow(null)
    val isWorking: StateFlow<Boolean>
        private field = MutableStateFlow(false)

    private var targetApkUrl: String? = null

    init {
        viewModelScope.launchIO {
            try {
                fetchInfo()
            } catch (t: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to check for updates!", t)
                mainThread { application.showToast(R.string.updater_check_fail) }
            }
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun triggerUpdate() = viewModelScope.launchIO {
        if (!isWorking.compareAndSet(expect = false, update = true))
            return@launchIO

        downloadProgress.value = null

        val url = targetApkUrl ?: return@launchIO
        val apkFile = application.cacheDir.resolve("manager.apk")

        try {
            apkFile.apply {
                parentFile!!.mkdirs()
                exists() && delete()
            }

            val downloadResult = downloader.download(
                url = url,
                out = apkFile,
                onProgressUpdate = { downloadProgress.value = it },
            )

            when (downloadResult) {
                is IDownloadManager.Result.Success ->
                    Log.d(BuildConfig.TAG, "Downloaded update")

                is IDownloadManager.Result.Cancelled -> {
                    Log.i(BuildConfig.TAG, "Update cancelled")
                    return@launchIO
                }

                is IDownloadManager.Result.Error ->
                    throw IllegalStateException("Failed to download update: ${downloadResult.getDebugReason()}", downloadResult.getError())
            }

            downloadProgress.value = null

            val installer = installers.getActiveInstaller()
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

            mainThread {
                application.showToast(R.string.updater_update_fail)
                launchReleasesPage()
            }
        } finally {
            isWorking.value = false

            try {
                apkFile.apply { exists() && delete() }
            } catch (t: Throwable) {
                Log.w(BuildConfig.TAG, "Failed to clean up installed update!", t)
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
        val (version, release, apkUrl) = releases
            .mapNotNull { release ->
                val version = SemVer.parseOrNull(release.tagName)
                    ?: return@mapNotNull null

                val asset = release.assets.find { it.name == "aliucord-manager-${release.tagName}.apk" }
                    ?: return@mapNotNull null

                Triple(version, release, asset.browserDownloadUrl)
            }
            .maxByOrNull { (version) -> version }
            ?: return

        // Check if currently installed version is greater
        if (currentVersion >= version) {
            Log.d(BuildConfig.TAG, "Already updated to latest version!")
            return
        }

        Log.d(BuildConfig.TAG, "Found an update! $targetVersion $targetApkUrl")
        mainThread {
            targetReleaseUrl = release.htmlUrl
            targetVersion = version.toString()
            targetApkUrl = apkUrl
            showDialog = true
        }
    }

    private fun launchReleasesPage() {
        try {
            Intent(Intent.ACTION_VIEW, targetReleaseUrl!!.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .also(application::startActivity)
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to open latest Github release in browser!", t)
        }
    }
}
