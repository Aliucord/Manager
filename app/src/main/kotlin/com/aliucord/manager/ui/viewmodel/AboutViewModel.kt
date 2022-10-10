package com.aliucord.manager.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.dto.GithubUser
import kotlinx.coroutines.launch

class AboutViewModel(
    private val githubRepository: GithubRepository
) : ViewModel() {
    val contributors = mutableStateListOf<GithubUser>()

    init {
        viewModelScope.launch {
            contributors.addAll(githubRepository.getContributors())
        }
    }
}
