package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.*
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(
    private val http: HttpService,
) {
    suspend fun getCommits(owner: String, repo: String, page: Int = 0): ApiResponse<List<Commit>> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://api.github.com/repos/$owner/$repo/commits")
                parameter("page", page)
            }
        }
    }

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

    suspend fun getLatestRelease(owner: String, repo: String): ApiResponse<GithubRelease> {
        return withContext(Dispatchers.IO) {
            http.request {
                url("https://api.github.com/repos/$owner/$repo/releases/latest")
            }
        }
    }
}
