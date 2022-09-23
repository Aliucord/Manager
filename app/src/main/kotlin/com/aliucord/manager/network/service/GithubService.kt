package com.aliucord.manager.network.service

import com.aliucord.manager.network.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GithubService(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun getCommits(page: Int): List<Commit> = withContext(Dispatchers.IO) {
        httpClient.get(COMMITS_URL) {
            parameter("page", page)
        }.body()
    }

    suspend fun getVersion(): Version = withContext(Dispatchers.IO) {
        // Return content type is plain text so manually decoding
        val res = httpClient.get(DATA_URL).bodyAsText()
        json.decodeFromString(res)
    }

    suspend fun getContributors(): List<GithubUser> = withContext(Dispatchers.IO) {
        httpClient.get(CONTRIBUTORS_URL).body()
    }

    suspend fun getHermesReleases(): List<GithubRelease> = withContext(Dispatchers.IO) {
        httpClient.get(HERMES_RELEASES_URL).body()
    }

    suspend fun getAliucordNativeReleases(): List<GithubRelease> = withContext(Dispatchers.IO) {
        httpClient.get(ALIUCORD_NATIVE_RELEASES_URL).body()
    }

    companion object {
        private const val ORG = "Aliucord"
        private const val REPO = "Aliucord"
        private const val HERMES_REPO = "Hermes"
        private const val NATIVE_REPO = "AliucordNative"

        private const val COMMITS_URL = "https://api.github.com/repos/$ORG/$REPO/commits"
        private const val CONTRIBUTORS_URL = "https://api.github.com/repos/$ORG/$REPO/contributors"
        private const val DATA_URL = "https://raw.githubusercontent.com/$ORG/$REPO/builds/data.json"

        private const val HERMES_RELEASES_URL = "https://api.github.com/repos/$ORG/$HERMES_REPO/releases"
        private const val ALIUCORD_NATIVE_RELEASES_URL = "https://api.github.com/repos/$ORG/$NATIVE_REPO/releases"
    }
}
