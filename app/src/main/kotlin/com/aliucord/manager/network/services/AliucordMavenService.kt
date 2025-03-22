package com.aliucord.manager.network.services

import com.aliucord.manager.network.utils.*
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode

class AliucordMavenService(private val http: HttpService) {
    suspend fun getAliuhookVersion(): ApiResponse<SemVer> {
        val metadataResponse = http.request<String> { url(ALIUHOOK_METADATA_URL) }

        return metadataResponse.transform {
            val versionString = "<release>(.+?)</release>".toRegex()
                .find(it)
                ?.groupValues?.get(1)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "No version in the aliuhook artifact metadata"))

            SemVer.parseOrNull(versionString)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "Invalid latest aliuhook version!"))
        }
    }

    companion object {
        private const val BASE_URL = "https://maven.aliucord.com/snapshots"
        private const val ALIUHOOK_METADATA_URL = "$BASE_URL/com/aliucord/Aliuhook/maven-metadata.xml"

        fun getAliuhookUrl(version: String): String =
            "$BASE_URL/com/aliucord/Aliuhook/$version/Aliuhook-$version.aar"
    }
}
