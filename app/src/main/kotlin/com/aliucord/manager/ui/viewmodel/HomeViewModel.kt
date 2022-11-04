package com.aliucord.manager.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.domain.manager.PreferencesManager
import com.aliucord.manager.domain.repository.GithubRepository
import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.network.utils.fold
import com.aliucord.manager.util.getPackageVersionCode
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.*

class HomeViewModel(
    private val application: Application,
    private val github: GithubRepository,
    val preferences: PreferencesManager
) : ViewModel() {
    var supportedVersion by mutableStateOf("")
        private set

    var supportedVersionType by mutableStateOf(VersionType.NONE)
        private set

    var installedVersion by mutableStateOf("")
        private set

    var installedVersionType by mutableStateOf(VersionType.NONE)
        private set

    val commits = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<Int, Commit>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Commit> {
                val page = params.key ?: 0

                return github.getCommits(page).fold(
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
            _fetchInstalledVersion()
            _fetchSupportedVersion()
        }
    }

    private suspend fun _fetchInstalledVersion() {
        try {
            val (versionName, versionCode) = application.getPackageVersionCode(preferences.packageName)

            withContext(Dispatchers.Main) {
                installedVersion = versionName.split("-")[0].trim()
                installedVersionType = VersionType.parseVersionCode(versionCode)
            }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, Log.getStackTraceString(t))

            withContext(Dispatchers.Main) {
                installedVersionType = VersionType.ERROR
            }
        }
    }

    private suspend fun _fetchSupportedVersion() {
        val version = github.getDataJson()

        withContext(Dispatchers.Main) {
            version.fold(
                success = {
                    supportedVersion = it.versionName
                    supportedVersionType = VersionType.parseVersionCode(it.versionCode.toIntOrNull())
                },
                fail = {
                    supportedVersionType = VersionType.ERROR
                }
            )
        }
    }

    fun fetchSupportedVersion() {
        viewModelScope.launch(Dispatchers.IO) {
            _fetchSupportedVersion()
        }
    }

    fun launchAliucord() {
        val launchIntent = application.packageManager
            .getLaunchIntentForPackage(preferences.packageName)

        if (launchIntent != null) {
            application.startActivity(launchIntent)
        } else {
            application.showToast(R.string.launch_aliucord_fail)
        }
    }

    fun uninstallAliucord() {
        val packageURI = Uri.parse("package:${preferences.packageName}")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        application.startActivity(uninstallIntent)
    }

    enum class VersionType {
        STABLE,
        BETA,
        ALPHA,
        UNKNOWN,
        NONE,
        ERROR;

        fun isVersion(): Boolean = when (this) {
            STABLE -> true
            BETA -> true
            ALPHA -> true
            else -> false
        }

        @Composable
        fun toDisplayName() = when (this) {
            UNKNOWN -> stringResource(R.string.version_unknown)
            STABLE -> stringResource(R.string.version_stable)
            BETA -> stringResource(R.string.version_beta)
            ALPHA -> stringResource(R.string.version_alpha)
            ERROR -> stringResource(R.string.version_load_fail)
            NONE -> stringResource(R.string.version_unknown)
        }

        companion object {
            fun parseVersionCode(versionCode: Int?): VersionType {
                return when (versionCode?.toString()?.get(3)) {
                    '0' -> STABLE
                    '1' -> BETA
                    '2' -> ALPHA
                    else -> UNKNOWN
                }
            }
        }
    }
}
