package com.aliucord.manager.network.service

import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.network.dto.Version
import com.aliucord.manager.network.utils.ApiResponse
import com.aliucord.manager.network.utils.transform
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AliucordGithubService(
    private val github: GithubService,
    private val http: HttpService,
    private val preferences: PreferencesManager,
) {
    suspend fun getDataJson(): ApiResponse<Version> {
        return withContext(Dispatchers.IO) {
            http.request {
                url(getDataJsonUrl())
            }
        }
    }

    suspend fun getLatestBootstrapCommit(): ApiResponse<String> {
        return withContext(Dispatchers.IO) {
            http.request<List<Commit>> {
                url("https://api.github.com/repos/$ORG/$RN_REPO/commits?sha=builds&path=bootstrap.js&per_page=1")
            }.transform {
                it.single().sha
            }
        }
    }

    suspend fun getCommits(page: Int = 0) = github.getCommits(ORG, MAIN_REPO, page)
    suspend fun getLatestHermesRelease() = github.getLatestRelease(ORG, HERMES_REPO)
    suspend fun getLatestAliucordNativeRelease() = github.getLatestRelease(ORG, ALIUNATIVE_REPO)
    suspend fun getManagerReleases() = github.getReleases(ORG, MANAGER_REPO)
    suspend fun getContributors() = github.getContributors(ORG, MAIN_REPO)

    private fun getBaseRawUrl() = if (preferences.httpOnly) {
        "http://raw.githubusercontent.com"
    } else {
        "https://raw.githubusercontent.com"
    }

    private fun getDataJsonUrl() = "${getBaseRawUrl()}/$ORG/$MAIN_REPO/builds/data.json"
    fun getKtInjectorUrl() = "${getBaseRawUrl()}/$ORG/$MAIN_REPO/builds/Injector.dex"
    fun getKtDexUrl() = "${getBaseRawUrl()}/$ORG/$MAIN_REPO/main/installer/android/app/src/main/assets/kotlin/classes.dex"
    fun getBootstrapUrl() = "${getBaseRawUrl()}/$ORG/$RN_REPO/builds/bootstrap.js"

    companion object {
        private const val ORG = "Aliucord"
        private const val MAIN_REPO = "Aliucord"
        private const val RN_REPO = "AliucordRN"
        private const val HERMES_REPO = "Hermes"
        private const val ALIUNATIVE_REPO = "AliucordNative"
        private const val MANAGER_REPO = "AliucordManager"
    }
}
