package com.aliucord.manager.network.services

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.di.cacheControl
import com.aliucord.manager.network.utils.*
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*

class AliucordMavenService(private val http: HttpService) {
    suspend fun getMavenMeta(artifactName: String, force: Boolean): ApiResponse<SemVer> {
        val metadataResponse = http.request<String> {
            url("${BuildConfig.MAVEN_URL}/com/aliucord/$artifactName/maven-metadata.xml")

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
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "No version in the $artifactName artifact metadata"))

            SemVer.parseOrNull(versionString)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "Invalid latest $artifactName version!"))
        }
    }

    suspend fun getAliuhookVersion(force: Boolean = false): ApiResponse<SemVer> =
        getMavenMeta("Aliuhook", force)

    suspend fun getAliuvoiceVersion(force: Boolean = false): ApiResponse<SemVer> =
        getMavenMeta("Aliuvoice", force)

    fun getAliuhookUrl(version: SemVer): String =
        "${BuildConfig.MAVEN_URL}/com/aliucord/Aliuhook/$version/Aliuhook-$version.aar"

    fun getAliuvoiceUrl(version: SemVer): String =
        "${BuildConfig.MAVEN_URL}/com/aliucord/Aliuvoice/$version/Aliuvoice-$version.aar"
}
