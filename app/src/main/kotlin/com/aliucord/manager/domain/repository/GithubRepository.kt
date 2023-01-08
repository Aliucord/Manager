package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.service.AliucordGithubService
import com.aliucord.manager.network.utils.transform

class GithubRepository(
    private val service: AliucordGithubService
) {
    suspend fun getCommits(page: Int = 0) = service.getCommits(page)
    suspend fun getContributors() = service.getContributors()
        .transform { it.sortedByDescending(GithubUser::contributions) }

    suspend fun getDataJson() = service.getDataJson()

    suspend fun getHermesRelease() = service.getLatestHermesRelease()
    suspend fun getAliucordNativeRelease() = service.getLatestAliucordNativeRelease()
    suspend fun getManagerReleases() = service.getManagerReleases()
    suspend fun getLatestBootstrapCommit() = service.getLatestBootstrapCommit()
}
