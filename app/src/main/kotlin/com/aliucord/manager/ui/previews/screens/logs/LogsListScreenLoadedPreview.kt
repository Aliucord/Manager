package com.aliucord.manager.ui.previews.screens.logs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.tooling.preview.Preview
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.logs.LogEntry
import com.aliucord.manager.ui.screens.logs.LogsScreenContent
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun LogsListScreenNonePreview() {
    ManagerTheme {
        LogsScreenContent(
            logs = logs,
            onOpenLog = {},
            onDeleteLogs = {},
        )
    }
}

private val logs: SnapshotStateList<LogEntry> = mutableStateListOf(
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = false,
        installDate = "5 min. ago, 10:18 AM",
        durationSecs = 18.555f,
        stacktracePreview = null,
    ),
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = true,
        installDate = "7 min. ago, 10:17 AM",
        durationSecs = 73.095f,
        stacktracePreview = persistentListOf(
            "kotlinx.coroutines.JobCancellationException: Job was cancelled; job=SupervisorJobImpl{Cancelling}@833e76f]",
        ),
    ),
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = false,
        installDate = "Yesterday, 11:37 PM",
        durationSecs = 58.439f,
        stacktracePreview = null,
    ),
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = true,
        installDate = "Yesterday, 11:17 PM",
        durationSecs = 24.405f,
        stacktracePreview = persistentListOf(
            "java.lang.Error: Installation was aborted or cancelled",
        ),
    ),
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = true,
        installDate = "Yesterday, 11:17 PM",
        durationSecs = 0.057f,
        stacktracePreview = persistentListOf(
            "java.lang.IllegalStateException: balls",
            "\tat com.aliucord.manager.patcher.steps.prepare.FetchInfoStep.execute(FetchInfoStep.kt:31)",
            "\tat com.aliucord.manager.patcher.steps.prepare.FetchInfoStep\$execute\\$1.invokeSuspend(Unknown Source:15)",
        ),
    ),
    LogEntry(
        id = UUID.randomUUID().toString(),
        isError = false,
        installDate = "Yesterday, 1:11 PM",
        durationSecs = 210.539f,
        stacktracePreview = null,
    ),
)
