package com.aliucord.manager.network.services

import com.aliucord.manager.di.cacheControl
import com.aliucord.manager.network.models.*
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import io.ktor.http.CacheControl

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
            cacheControl(CacheControl.NoCache(null))
        }
    }

    /**
     * Fetches all the Manager releases with a 60s local cache.
     */
    suspend fun getManagerReleases(): ApiResponse<List<GithubRelease>> {
        return http.request {
            url("https://api.github.com/repos/$ORG/$MANAGER_REPO/releases")
            cacheControl(CacheControl.MaxAge(maxAgeSeconds = 60))
        }
    }

    /**
     * Fetches all the contributors with a 24h local cache.
     */
    suspend fun getContributors(): ApiResponse<List<Contributor>> = http.request {
        url(CONTRIBUTORS_API_URL)
        cacheControl(CacheControl.MaxAge(maxAgeSeconds = 60 * 60 * 24))
    }

    companion object {
        const val ORG = "Aliucord"
        const val MANAGER_REPO = "Manager"

        const val DATA_JSON_URL = "https://builds.aliucord.com/data.json"

        private const val CONTRIBUTORS_API_URL = "https://raw.githubusercontent.com/Importantamigo/guhhhhggh/main/contributors_data/all_contributors.json"
    }
}
