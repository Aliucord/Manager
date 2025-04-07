package com.aliucord.manager.ui.screens.log

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.InstallLogData
import com.aliucord.manager.manager.InstallLogManager
import com.aliucord.manager.util.*

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

        val formattedDate = data.getFormattedInstallDate()
        val content = data.getLogFileContents()

        application.saveFile("Aliucord Install $formattedDate.log", content)
    }

    /**
     * Writes the log to internal cache and launches a share intent of the log file.
     */
    fun shareLog() = screenModelScope.launchBlock {
        val data = data ?: return@launchBlock
        val formattedDate = data.getFormattedInstallDate()
        val formattedName = "Aliucord Install $formattedDate.log"
        val content = data.getLogFileContents()

        val file = application.cacheDir.resolve(formattedName)
        val fileUri = FileProvider.getUriForFile(
            /* context = */ application,
            /* authority = */ "${BuildConfig.APPLICATION_ID}.provider",
            /* file = */ file,
            /* displayName = */ formattedName,
        )

        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/*")
            .putExtra(Intent.EXTRA_STREAM, fileUri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .let {
                Intent.createChooser(
                    /* target = */ it,
                    /* title = */ application.getString(R.string.log_action_share),
                )
            }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            file.writeText(content)
            file.deleteOnExit()
            application.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(BuildConfig.TAG, "Failed to share log", t)
            application.showToast(R.string.status_failed)
        }
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
