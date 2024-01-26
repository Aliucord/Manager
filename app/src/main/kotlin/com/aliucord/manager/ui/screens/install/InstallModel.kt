package com.aliucord.manager.ui.screens.install

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.KotlinInstallContainer
import com.aliucord.manager.installer.util.*
import com.aliucord.manager.manager.*
import com.aliucord.manager.util.*
import com.github.diamondminer88.zip.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

class InstallModel(
    private val application: Application,
    private val paths: PathManager,
) : ScreenModel {
    private val installationRunning = AtomicBoolean(false)

    var returnToHome by mutableStateOf(false)

    var isFinished by mutableStateOf(false)
        private set

    var stacktrace by mutableStateOf("")
        private set

    private val debugInfo: String
        get() = """
            Aliucord Manager ${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} ${if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""}

            Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}

            Failed on: ${currentStep?.name}
        """.trimIndent()

    fun copyDebugToClipboard() {
        val text = "$debugInfo\n\n$stacktrace"
            // TODO: remove this useless replace
            .replace("(\\\\*~_)".toRegex(), "\\$1")

        application.copyToClipboard(text)
        application.showToast(R.string.action_copied)
    }

    private var debugLogPath by mutableStateOf<String?>(null)

    @SuppressLint("SimpleDateFormat")
    fun saveDebugToFile() {
        val name = if (debugLogPath != null) {
            debugLogPath!!
        } else {
            "Aliucord Manager ${SimpleDateFormat("yyyy-MM-dd hh-mm-s a").format(Date())}.log"
                .also { debugLogPath = it }
        }

        application.saveFile(name, "$debugInfo\n\n$stacktrace")
    }

    fun clearCache() {
        paths.clearCache()
        application.showToast(R.string.action_cleared_cache)
    }

    private val installJob = screenModelScope.launch(Dispatchers.Main) {
        if (installationRunning.getAndSet(true)) {
            return@launch
        }

        withContext(Dispatchers.IO) {
            try {
                installKotlin()

                isFinished = true
                delay(20000)
                returnToHome = true
            } catch (t: Throwable) {
                stacktrace = Log.getStackTraceString(t)

                Log.e(
                    BuildConfig.TAG,
                    "$debugInfo\n\n${Log.getStackTraceString(t)}"
                )
            }

            installationRunning.set(false)
        }
    }

    private suspend fun installKotlin() {
        val error = KotlinInstallContainer().executeAll()

        if (error != null) {
            Log.e(BuildConfig.TAG, "Failed to perform installation process", error)
        }
    }
}
