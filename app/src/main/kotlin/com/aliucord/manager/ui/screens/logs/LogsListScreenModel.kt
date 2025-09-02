package com.aliucord.manager.ui.screens.logs

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.R
import com.aliucord.manager.manager.InstallLogManager
import com.aliucord.manager.util.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class LogsListScreenModel(
    private val logsManager: InstallLogManager,
    private val application: Application,
) : ScreenModel {
    /**
     * All the loaded log entries sorted descending by creation date.
     */
    val logEntries = mutableStateListOf<LogEntry>()

    init {
        loadLogsList()
    }

    fun deleteLogs() = screenModelScope.launchIO {
        logsManager.deleteAllEntries()

        mainThread {
            logEntries.clear()
            application.showToast(R.string.logs_status_delete_success)
        }
    }

    private fun loadLogsList() = screenModelScope.launchIO {
        for (installId in logsManager.fetchInstallDataEntries()) {
            val data = logsManager.fetchInstallData(id = installId)
                ?: continue

            val entry = LogEntry(
                id = data.id,
                isError = data.isError,
                installDate = DateUtils.getRelativeDateTimeString(
                    /* c = */ application,
                    /* time = */ data.installDate.toEpochMilliseconds(),
                    /* minResolution = */ DateUtils.SECOND_IN_MILLIS,
                    /* transitionResolution = */ DateUtils.WEEK_IN_MILLIS,
                    /* flags = */ DateUtils.FORMAT_ABBREV_ALL,
                ).toString(),
                durationSecs = data.installDuration.inWholeMilliseconds / 1000f,
                stacktracePreview = data.errorStacktrace
                    ?.splitToSequence('\n')
                    ?.take(3)
                    ?.toImmutableList(),
            )

            mainThread { logEntries += entry }
        }
    }
}

@Immutable
data class LogEntry(
    val id: String,
    val isError: Boolean,
    val installDate: String,
    val durationSecs: Float,
    val stacktracePreview: ImmutableList<String>?,
)
