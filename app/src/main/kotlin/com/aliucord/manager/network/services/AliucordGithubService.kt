package com.aliucord.manager.network.services

import com.aliucord.manager.network.models.BuildInfo
import com.aliucord.manager.network.models.GithubRelease
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders

class AliucordGithubService(
    private val http: HttpService,
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

    /**
     * Fetches all the Manager releases with a 60s local cache.
     */
    suspend fun getManagerReleases(): ApiResponse<List<GithubRelease>> {
        return http.request {
            url("https://api.github.com/repos/rushiiMachine/$MANAGER_REPO/releases")
            header(HttpHeaders.CacheControl, "public, max-age=60, s-maxage=60")
        }
    }

    companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"
        const val MANAGER_REPO = "Manager"

        const val DATA_JSON_URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/data.json"
    }
}
