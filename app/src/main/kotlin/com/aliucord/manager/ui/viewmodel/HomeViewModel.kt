package com.aliucord.manager.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.network.utils.fold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val application: Application,
    private val githubRepository: GithubRepository,
    val preferences: PreferencesManager
) : ViewModel() {
    private val packageManager = application.packageManager

    var supportedVersion by mutableStateOf("")
        private set

    var installedVersion by mutableStateOf("-")
        private set

    val commits = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<Int, Commit>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Commit> {
                val page = params.key ?: 0

                return githubRepository.getCommits(page).fold(
                    success = { commits ->
                        val prevKey = if (page > 0) page - 1 else null
                        val nextKey = if (commits.isNotEmpty()) page + 1 else null

                        LoadResult.Page(
                            data = commits,
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    },
                    fail = { LoadResult.Error(it) }
                )
            }

            override fun getRefreshKey(state: PagingState<Int, Commit>) = state.anchorPosition?.let {
                state.closestPageToPosition(it)?.prevKey?.plus(1) ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
            }
        }
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _fetchSupportedVersion()

            installedVersion = try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(preferences.packageName, 0).versionName
            } catch (th: Throwable) {
                "-"
            }
        }
    }

    private suspend fun _fetchSupportedVersion() {
        val version = githubRepository.getVersion()

        supportedVersion = version.fold(
            success = {
                "${it.versionName} - " + when (it.versionCode[3]) {
                    '0' -> "Stable"
                    '1' -> "Beta"
                    '2' -> "Alpha"
                    else -> throw NoWhenBranchMatchedException()
                }
            },
            fail = { "Failed to retrieve version" }
        )
    }

    fun fetchSupportedVersion() {
        viewModelScope.launch(Dispatchers.IO) {
            _fetchSupportedVersion()
        }
    }

    fun launchAliucord() {
        val launchIntent = packageManager.getLaunchIntentForPackage(preferences.packageName)

        if (launchIntent != null) {
            application.startActivity(launchIntent)
        } else {
            Toast.makeText(application, "Failed to launch Aliucord", Toast.LENGTH_LONG).show()
        }
    }

    fun uninstallAliucord() {
        val packageURI = Uri.parse("package:${preferences.packageName}")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        application.startActivity(uninstallIntent)
    }
}
