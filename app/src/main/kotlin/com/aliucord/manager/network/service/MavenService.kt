package com.aliucord.manager.network.service

import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MavenService(
    private val http: HttpService,
) {
    suspend fun getArtifactMetadata(baseUrl: String, artifactCoords: String): ApiResponse<String> = withContext(Dispatchers.IO) {
        http.request {
            val coords = artifactCoords.replace("[:.]".toRegex(), "/")
            url("$baseUrl/${coords}/maven-metadata.xml")
        }
    }
}
