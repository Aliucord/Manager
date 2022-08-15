package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.network.dto.GithubUser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GithubService(
    private val json: Json
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getCommits(page: Int): List<Commit> = withContext(Dispatchers.IO) {
        httpClient.get(commitsUrl) {
            parameter("page", page)
        }.body()
    }

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        httpClient.get(dataUrl).bodyAsText()
    }

    suspend fun getContributors(): List<GithubUser> = withContext(Dispatchers.IO) {
        httpClient.get(contributorsUrl).body()
    }

    companion object {
        private const val org = "Aliucord"
        private const val repo = "Aliucord"

        private const val commitsUrl = "https://api.github.com/repos/$org/$repo/commits"
        private const val contributorsUrl = "https://api.github.com/repos/$org/$repo/contributors"
        private const val dataUrl = "https://raw.githubusercontent.com/$org/$repo/builds/data.json"
    }
}
