package com.aliucord.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.dto.GithubUser
import com.aliucord.manager.network.utils.fold
import kotlinx.coroutines.launch

class AboutViewModel(
    private val githubRepository: GithubRepository
) : ViewModel() {
    val contributors = mutableStateListOf<GithubUser>()
    var fetchError by mutableStateOf(false)

    init {
        fetchContributors()
    }

    fun fetchContributors() {
        viewModelScope.launch {
            githubRepository.getContributors().fold(
                success = {
                    contributors.addAll(it)
                    fetchError = false
                },
                fail = { fetchError = true }
            )
        }
    }
}
