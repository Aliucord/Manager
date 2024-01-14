package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.Version
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AliucordGithubService(
    val github: GithubService,
    val http: HttpService,
) {
    suspend fun getDataJson(): ApiResponse<Version> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/data.json")
            }
        }
    }

    suspend fun getManagerReleases() = github.getReleases(ORG, MANAGER_REPO)
    suspend fun getContributors() = github.getContributors(ORG, MAIN_REPO)

    companion object {
        private const val ORG = "Aliucord"
        private const val MAIN_REPO = "Aliucord"
        private const val MANAGER_REPO = "Manager"

        const val KT_INJECTOR_URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/Injector.dex"
        const val KOTLIN_DEX_URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/main/installer/android/app/src/main/assets/kotlin/classes.dex"
    }
}
