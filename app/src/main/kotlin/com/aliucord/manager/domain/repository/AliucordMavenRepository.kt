package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.service.MavenService
import com.aliucord.manager.network.utils.*
import io.ktor.http.HttpStatusCode

class AliucordMavenRepository(
    private val maven: MavenService,
) {
    suspend fun getAliuhookVersion(): ApiResponse<SemVer> {
        return maven.getArtifactMetadata(BASE_URL, ALIUHOOK).transform {
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
        private const val ALIUHOOK = "com.aliucord:Aliuhook"

        fun getAliuhookUrl(version: String): String =
            "$BASE_URL/com/aliucord/Aliuhook/$version/Aliuhook-$version.aar"
    }
}
