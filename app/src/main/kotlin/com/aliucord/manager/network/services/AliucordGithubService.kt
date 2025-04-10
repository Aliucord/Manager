package com.aliucord.manager.network.services

import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders

class AliucordGithubService(
    val github: GithubService,
    val http: HttpService,
) {
    /**
     * Fetches the build data of Aliucord (excluding Aliuhook).
     * @param force Whether to force Ktor to refetch/revalidate cache.
     */
    suspend fun getBuildData(force: Boolean = false): ApiResponse<BuildInfo> = http.request {
        url(DATA_JSON_URL)

        if (force) {
            header(HttpHeaders.CacheControl, "no-cache")
        }
    }

    suspend fun getManagerReleases() = github.getReleases(ORG, MANAGER_REPO)

    companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"
        const val MANAGER_REPO = "Manager"

        const val DATA_JSON_URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/data.json"
        const val PATCHED_APKS_INFO_URL = "https://github.com/$ORG/$MANAGER_REPO/blob/main/INFO.md#obtaining-patched-apks"
    }
}
