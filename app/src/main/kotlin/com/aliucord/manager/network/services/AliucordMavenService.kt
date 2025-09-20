package com.aliucord.manager.network.services

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.di.cacheControl
import com.aliucord.manager.network.utils.*
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*

class AliucordMavenService(private val http: HttpService) {
    suspend fun getAliuhookVersion(force: Boolean = false): ApiResponse<SemVer> {
        val metadataResponse = http.request<String> {
            url(ALIUHOOK_METADATA_URL)

            if (!force) {
                cacheControl(CacheControl.MaxAge(maxAgeSeconds = 60 * 30)) // 30 min
            } else {
                header(HttpHeaders.CacheControl, "no-cache")
            }
        }

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
        private const val ALIUHOOK_METADATA_URL = "${BuildConfig.MAVEN_URL}/com/aliucord/Aliuhook/maven-metadata.xml"

        fun getAliuhookUrl(version: String): String =
            "${BuildConfig.MAVEN_URL}/com/aliucord/Aliuhook/$version/Aliuhook-$version.aar"
    }
}
