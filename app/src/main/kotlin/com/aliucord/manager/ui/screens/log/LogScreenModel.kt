package com.aliucord.manager.ui.screens.log

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.R
import com.aliucord.manager.manager.InstallLogData
import com.aliucord.manager.manager.InstallLogManager
import com.aliucord.manager.util.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogScreenModel(
    private val installId: String,
    private val logs: InstallLogManager,
    private val application: Application,
) : ScreenModel {
    var shouldCloseScreen by mutableStateOf(false)
        private set

    var data by mutableStateOf<InstallLogData?>(null)
        private set

    init {
        loadLogData()
    }

    /**
     * Formats the log data into a file and writes it to the downloads folder.
     */
    fun saveLog() = screenModelScope.launchBlock {
        val data = data ?: return@launchBlock

        @SuppressLint("SimpleDateFormat")
        val formattedDate = SimpleDateFormat("yyyy-MM-dd hh-mm-s a", Locale.ENGLISH)
            .format(Date(data.installDate.toEpochMilliseconds()))

        val content = buildString {
            appendLine("////////////////// Installation Info //////////////////")
            append("Install ID: ")
            appendLine(data.id)
            append("Install time: ")
            appendLine(formattedDate)
            append("Result: ")
            appendLine(if (data.isError) "Failure" else "Success")

            append("\n\n")
            appendLine("////////////////// Environment Info //////////////////")
            appendLine(data.environmentInfo)

            append("\n\n")
            appendLine("////////////////// Error Stacktrace //////////////////")
            appendLine(data.errorStacktrace ?: "None")

            append("\n\n")
            appendLine("////////////////// Installation Log //////////////////")
            appendLine(data.installationLog)
        }

        application.saveFile("Aliucord Manager $formattedDate.log", content)
    }

    private fun loadLogData() = screenModelScope.launchBlock {
        val result = logs.fetchInstallData(installId)

        if (result != null) {
            data = result
        } else {
            shouldCloseScreen = true
            application.showToast(R.string.network_load_fail)
        }
    }
}
