package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.GithubRelease
import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(
    private val http: HttpService,
) {
    suspend fun getContributors(owner: String, repo: String): ApiResponse<List<GithubUser>> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://api.github.com/repos/$owner/$repo/contributors")
            }
        }
    }

    suspend fun getReleases(owner: String, repo: String): ApiResponse<List<GithubRelease>> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://api.github.com/repos/$owner/$repo/releases")
            }
        }
    }
}
