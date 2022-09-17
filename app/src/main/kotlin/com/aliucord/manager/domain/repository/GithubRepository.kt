package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.dto.Version
import com.aliucord.manager.network.service.GithubService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GithubRepository(
    private val service: GithubService
) {
    suspend fun getCommits(page: Int) = service.getCommits(page)

    suspend fun getContributors() = service.getContributors()
        .sortedByDescending(GithubUser::contributions)

    suspend fun getVersion() = service.getVersion()
}
