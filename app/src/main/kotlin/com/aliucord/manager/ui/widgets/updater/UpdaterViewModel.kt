package com.aliucord.manager.ui.widgets.updater

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.installer.util.installApks
import com.aliucord.manager.manager.DownloadManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdaterViewModel(
    private val github: GithubRepository,
    private val downloadManager: DownloadManager,
    private val application: Application,
) : ViewModel() {
    var showDialog by mutableStateOf(false)
        private set
    var targetVersion by mutableStateOf<String?>(null)
        private set
    var isWorking by mutableStateOf(false)
        private set

    private var targetApkUrl: String? = null

    fun dismissDialog() {
        showDialog = false
    }

    fun triggerUpdate() {
        if (isWorking) return
        val url = targetApkUrl ?: return

        viewModelScope.launch {
            isWorking = true

            application.externalCacheDir!!.resolve("manager.apk").also {
                it.delete()

                downloadManager.download(url, it)
                application.installApks(silent = true, it)

                it.delete()
            }
        }
    }

    // This updates to the latest release based on the tag name
    // It finds all releases that have an asset named aliucord-manager-[tagname].apk (this is in case the filename changes in the future)
    // Then compares the semantic versioning of the tag name which should be in the v1.0.0 format
    // Finds the latest and downloads/installs it
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val (version, asset) = github.getManagerReleases().getOrNull()
                ?.mapNotNull { release ->
                    val version = SemVer.parse(release.tagName, vPrefix = true)
                        ?: return@mapNotNull null

                    val asset = release.assets.find { it.name == "aliucord-manager-${release.tagName}.apk" }
                        ?: return@mapNotNull null

                    version to asset
                }
                ?.maxByOrNull { it.first }
                ?: return@launch

            val currentVersion = SemVer.parse(BuildConfig.VERSION_NAME)
                ?: throw Error("Failed to parse app version")

            if (currentVersion >= version)
                return@launch

            targetVersion = version.toString()
            targetApkUrl = asset.browserDownloadUrl
            showDialog = true
        }
    }
}
