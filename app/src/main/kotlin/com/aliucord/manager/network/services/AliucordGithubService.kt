package com.aliucord.manager.network.services

import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AliucordGithubService(
    val github: GithubService,
    val http: HttpService,
) {
    suspend fun getDataJson(): ApiResponse<BuildInfo> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/data.json")
            }
        }
    }

    suspend fun getManagerReleases() = github.getReleases(ORG, MANAGER_REPO)

    companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"
        const val MANAGER_REPO = "Manager"

        const val PATCHED_APKS_INFO_URL = "https://github.com/$ORG/$MANAGER_REPO/blob/main/INFO.md#obtaining-patched-apks"
    }
}
