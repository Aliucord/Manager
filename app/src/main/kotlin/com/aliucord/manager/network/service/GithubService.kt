package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.*
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(
    private val http: HttpService,
) {
    suspend fun getCommits(page: Int): ApiResponse<List<Commit>> = withContext(Dispatchers.IO) {
        http.request {
            url(COMMITS_URL)
            parameter("page", page)
        }
    }

    suspend fun getVersion(): ApiResponse<Version> = withContext(Dispatchers.IO) {
        http.request {
            url(DATA_URL)
        }
    }

    suspend fun getContributors(): ApiResponse<List<GithubUser>> = withContext(Dispatchers.IO) {
        http.request {
            url(CONTRIBUTORS_URL)
        }
    }

    suspend fun getHermesReleases(): ApiResponse<List<GithubRelease>> = withContext(Dispatchers.IO) {
        http.request {
            url(HERMES_RELEASES_URL)
        }
    }

    suspend fun getAliucordNativeReleases(): ApiResponse<List<GithubRelease>> = withContext(Dispatchers.IO) {
        http.request {
            url(ALIUCORD_NATIVE_RELEASES_URL)
        }
    }

    suspend fun getManagerReleases(): ApiResponse<List<GithubRelease>> = withContext(Dispatchers.IO) {
        http.request {
            url(MANAGER_RELEASES_URL)
        }
    }

    companion object {
        private const val ORG = "Aliucord"
        private const val REPO = "Aliucord"
        private const val HERMES_REPO = "Hermes"
        private const val NATIVE_REPO = "AliucordNative"
        private const val MANAGER_REPO = "AliucordManager"

        private const val COMMITS_URL = "https://api.github.com/repos/$ORG/$REPO/commits"
        private const val CONTRIBUTORS_URL = "https://api.github.com/repos/$ORG/$REPO/contributors"

        private const val DATA_URL = "https://raw.githubusercontent.com/$ORG/$REPO/builds/data.json"
        const val KT_INJECTOR_URL = "https://raw.githubusercontent.com/$ORG/$REPO/builds/Injector.dex"
        const val KOTLIN_DEX_URL = "https://raw.githubusercontent.com/$ORG/$REPO/main/installer/android/app/src/main/assets/kotlin/classes.dex"

        private const val HERMES_RELEASES_URL = "https://api.github.com/repos/$ORG/$HERMES_REPO/releases"
        private const val ALIUCORD_NATIVE_RELEASES_URL = "https://api.github.com/repos/$ORG/$NATIVE_REPO/releases"

        const val MANAGER_RELEASES_URL = "https://api.github.com/repos/$ORG/$MANAGER_REPO/releases"
    }
}
