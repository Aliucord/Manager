package com.aliucord.manager.ui.screens.about

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.network.services.AliucordGithubService
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.launchIO

class AboutModel(
    private val aliucordGithubService: AliucordGithubService,
) : StateScreenModel<AboutScreenState>(AboutScreenState.Loading) {
    init {
        fetchContributors()
    }

    fun fetchContributors() = screenModelScope.launchIO {
        mutableState.value = AboutScreenState.Loading

        val response = aliucordGithubService.getContributors()

        mutableState.value = response.fold(
            success = { contributors ->
                val sorted = contributors
                    .sortedByDescending { it.commits }
                    .toUnsafeImmutable()

                AboutScreenState.Loaded(sorted)
            },
            fail = { AboutScreenState.Failure },
        )
    }
}
