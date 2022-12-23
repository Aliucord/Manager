package com.aliucord.manager.domain.repository

import com.aliucord.manager.BuildConfig
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.network.service.MavenService
import com.aliucord.manager.network.utils.ApiError
import com.aliucord.manager.network.utils.ApiResponse
import com.aliucord.manager.network.utils.transform
import io.ktor.http.*

class AliucordMavenRepository(
    private val maven: MavenService,
    private val preferences: PreferencesManager,
) {
    private fun getBaseUrl() = if (preferences.httpOnly) {
        BuildConfig.MAVEN_REPO_URL.replace("https", "http")
    } else {
        BuildConfig.MAVEN_REPO_URL
    }

    suspend fun getAliuhookVersion(): ApiResponse<String> {
        return maven.getArtifactMetadata(getBaseUrl(), ALIUHOOK).transform {
            "<release>(.+?)</release>".toRegex()
                .find(it)
                ?.groupValues?.get(1)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "No version in the aliuhook artifact metadata"))
        }
    }

    fun getAliuhookUrl(version: String): String {
        return MavenService.getAARUrl(getBaseUrl(), "$ALIUHOOK:$version")
    }

    companion object {
        private const val ALIUHOOK = "com.aliucord:Aliuhook"
    }
}
