package com.aliucord.manager.ui.screens.about

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.network.models.Contributor
import com.aliucord.manager.network.services.HttpService
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.util.launchBlock
import io.ktor.client.request.url

class AboutModel(
    private val http: HttpService,
) : ScreenModel {
    val contributors = mutableStateListOf<Contributor>()
    var fetchError by mutableStateOf(false)

    init {
        fetchContributors()
    }

    fun fetchContributors() = screenModelScope.launchBlock {
        val response = http.request<List<Contributor>> { url(CONTRIBUTORS_API_URL) }

        response.fold(
            success = {
                contributors.clear()
                contributors.addAll(it)
                fetchError = false
            },
            fail = { fetchError = true },
        )
    }

    companion object {
        private const val CONTRIBUTORS_API_URL = "https://api.rushii.dev/aliucord/contributors"
    }
}
