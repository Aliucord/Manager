package com.aliucord.manager.network.services

import com.aliucord.manager.network.models.GithubRelease
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(
    private val http: HttpService,
) {
    suspend fun getReleases(owner: String, repo: String): ApiResponse<List<GithubRelease>> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://api.github.com/repos/$owner/$repo/releases")
            }
        }
    }
}
