package com.aliucord.manager.ui.screens.about

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.utils.fold
import kotlinx.coroutines.launch

class AboutModel(
    private val githubRepository: GithubRepository,
) : ScreenModel {
    val contributors = mutableStateListOf<GithubUser>()
    var fetchError by mutableStateOf(false)

    init {
        fetchContributors()
    }

    fun fetchContributors() {
        screenModelScope.launch {
            githubRepository.getContributors().fold(
                success = {
                    contributors.clear()
                    contributors.addAll(it)
                    fetchError = false
                },
                fail = { fetchError = true },
            )
        }
    }
}
