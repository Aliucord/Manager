package com.aliucord.manager.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.model.github.GithubUser
import com.aliucord.manager.util.Github
import com.aliucord.manager.util.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch

class AboutViewModel : ViewModel() {

    val contributors = mutableStateListOf<GithubUser>()

    fun load() {
        viewModelScope.launch {
            val githubContributors = httpClient.get(Github.contributorsUrl).body<List<GithubUser>>()
                .sortedByDescending {
                    it.contributions
                }
            contributors.addAll(githubContributors)
        }
    }

    init {
        load()
    }

}
