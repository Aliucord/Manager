package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.service.GithubService
import com.aliucord.manager.network.utils.transform

class GithubRepository(
    private val service: GithubService
) {
    suspend fun getCommits(page: Int) = service.getCommits(page)

    suspend fun getContributors() = service.getContributors()
        .transform { it.sortedByDescending(GithubUser::contributions) }

    suspend fun getDiscordKtVersion() = service.getVersion()

    suspend fun getHermesReleases() = service.getHermesReleases()

    suspend fun getAliucordNativeReleases() = service.getAliucordNativeReleases()
}
