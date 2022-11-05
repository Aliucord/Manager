package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.service.MavenService
import com.aliucord.manager.network.utils.ApiError
import com.aliucord.manager.network.utils.ApiResponse
import com.aliucord.manager.network.utils.transform
import io.ktor.http.*

class AliucordMavenRepository(
    private val maven: MavenService
) {
    suspend fun getAliuhookVersion(): ApiResponse<String> {
        return maven.getArtifactMetadata(BASE_URL, ALIUHOOK).transform {
            "<release>(.+?)</release>".toRegex()
                .find(it)
                ?.groupValues?.get(1)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "No version in the aliuhook artifact metadata"))
        }
    }

    companion object {
        private const val BASE_URL = "https://maven.aliucord.com/snapshots"
        private const val ALIUHOOK = "com.aliucord:Aliuhook"

        fun getAliuhookUrl(version: String): String {
            return MavenService.getAARUrl(BASE_URL, "$ALIUHOOK:$version")
        }
    }
}
