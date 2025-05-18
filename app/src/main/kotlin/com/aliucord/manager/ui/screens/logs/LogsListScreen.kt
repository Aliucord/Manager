package com.aliucord.manager.ui.screens.logs

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.ui.screens.log.LogScreen
import com.aliucord.manager.ui.screens.logs.components.*
import com.aliucord.manager.ui.screens.logs.components.dialogs.DeleteLogsDialog
import com.aliucord.manager.ui.util.paddings.PaddingValuesSides
import com.aliucord.manager.ui.util.paddings.exclude
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class LogsListScreen : Screen, Parcelable {
    @IgnoredOnParcel
    override val key: ScreenKey
        get() = "LogsScreen"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<LogsListScreenModel>()

        LogsScreenContent(
            logs = model.logEntries,
            onOpenLog = { navigator.push(LogScreen(installId = it)) },
            onDeleteLogs = model::deleteLogs, // TODO: show warning dialog first
        )
    }
}

@Composable
fun LogsScreenContent(
    logs: SnapshotStateList<LogEntry>,
    onOpenLog: (id: String) -> Unit,
    onDeleteLogs: () -> Unit,
) {
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    if (showWipeConfirmDialog) {
        DeleteLogsDialog(
            onConfirm = {
                showWipeConfirmDialog = false
                onDeleteLogs()
            },
            onDismiss = {
                showWipeConfirmDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            LogsListAppBar(
                onDeleteLogs = { showWipeConfirmDialog = true },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues.exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top),
            modifier = Modifier
                .padding(paddingValues.exclude(PaddingValuesSides.Bottom))
                .padding(vertical = 12.dp, horizontal = 22.dp)
        ) {
            if (logs.isEmpty()) {
                item(key = "EMPTY") {
                    LogsNone(
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }

            items(
                items = logs,
                contentType = { "LOG" },
                key = { it.id },
            ) { data ->
                LogEntryCard(
                    data = data,
                    onClick = remember(onOpenLog, data.id) { { onOpenLog(data.id) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
