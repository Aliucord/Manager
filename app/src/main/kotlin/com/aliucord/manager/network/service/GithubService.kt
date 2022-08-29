package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.network.dto.GithubUser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(
    private val httpClient: HttpClient
) {
    suspend fun getCommits(page: Int): List<Commit> = withContext(Dispatchers.IO) {
        httpClient.get(COMMITS_URL) {
            parameter("page", page)
        }.body()
    }

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        httpClient.get(DATA_URL).bodyAsText()
    }

    suspend fun getContributors(): List<GithubUser> = withContext(Dispatchers.IO) {
        httpClient.get(CONTRIBUTORS_URL).body()
    }

    companion object {
        private const val ORG = "Aliucord"
        private const val REPO = "Aliucord"

        private const val COMMITS_URL = "https://api.github.com/repos/$ORG/$REPO/commits"
        private const val CONTRIBUTORS_URL = "https://api.github.com/repos/$ORG/$REPO/contributors"
        private const val DATA_URL = "https://raw.githubusercontent.com/$ORG/$REPO/builds/data.json"
    }
}
