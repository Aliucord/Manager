package com.aliucord.manager.domain.repository

import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.service.AliucordGithubService
import com.aliucord.manager.network.utils.transform

class GithubRepository(
    private val service: AliucordGithubService,
) {
    suspend fun getContributors() = service.getContributors()
        .transform { it.sortedByDescending(GithubUser::contributions) }

    suspend fun getDataJson() = service.getDataJson()

    suspend fun getManagerReleases() = service.getManagerReleases()
}
