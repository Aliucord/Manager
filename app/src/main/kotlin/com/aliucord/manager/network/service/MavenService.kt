package com.aliucord.manager.network.service

import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.request.*
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

    companion object {
        fun getAARUrl(baseUrl: String, coords: String): String {
            val parts = coords.split("[:.]".toRegex())
            val artifact = parts[parts.lastIndex - 1]
            val version = parts.last()
            return "$baseUrl/${parts.joinToString("/")}/$artifact-$version.aar"
        }
    }
}
