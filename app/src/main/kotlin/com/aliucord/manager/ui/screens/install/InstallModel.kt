package com.aliucord.manager.ui.screens.install

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.KotlinInstallRunner
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date

class InstallModel(
    private val application: Application,
    private val paths: PathManager,
) : StateScreenModel<InstallScreenState>(InstallScreenState.Pending) {
    private lateinit var startTime: Date
    private var installJob: Job? = null

    var installSteps by mutableStateOf<ImmutableMap<StepGroup, ImmutableList<Step>>?>(null)
        private set

    init {
        restart()
    }

    fun copyDebugToClipboard() {
        val content = (state.value as? InstallScreenState.Failed)?.failureLog
            ?: return

        application.copyToClipboard(content)
        application.showToast(R.string.action_copied)
    }

    fun saveFailureLog() {
        val failureLog = (state.value as? InstallScreenState.Failed)?.failureLog
            ?: return

        @SuppressLint("SimpleDateFormat")
        val formattedDate = SimpleDateFormat("yyyy-MM-dd hh-mm-s a").format(startTime)
        val fileName = "Aliucord Manager $formattedDate.log"

        application.saveFile(fileName, failureLog)
    }

    fun clearCache() {
        paths.clearCache()
        application.showToast(R.string.action_cleared_cache)
    }

    fun restart() {
        installJob?.cancel("Manual cancellation")
        installSteps = null

        startTime = Date()
        mutableState.value = InstallScreenState.Working

        val newInstallJob = screenModelScope.launch {
            val runner = KotlinInstallRunner()

            installSteps = runner.steps.groupBy { it.group }
                .mapValues { it.value.toUnsafeImmutable() }
                .toUnsafeImmutable()

            // Execute all the steps and catch any errors
            when (val error = runner.executeAll()) {
                // Successfully installed
                null -> {
                    mutableState.value = InstallScreenState.Success

                    // Wait 20s before returning to Home
                    delay(20_000)
                    mutableState.value = InstallScreenState.CloseScreen
                }

                else -> {
                    Log.e(BuildConfig.TAG, "Failed to perform installation process", error)

                    mutableState.value = InstallScreenState.Failed(failureLog = getFailureInfo(error))
                }
            }
        }

        newInstallJob.invokeOnCompletion { error ->
            when (error) {
                // Successfully executed, already handled above
                null -> {}

                // Job was cancelled before being able to finish setting state
                is CancellationException -> {
                    Log.w(BuildConfig.TAG, "Installation was cancelled before completing", error)
                    mutableState.value = InstallScreenState.CloseScreen
                }

                // This should never happen, all install errors are caught
                else -> throw error
            }
        }

        installJob = newInstallJob
    }

    private fun getFailureInfo(stacktrace: Throwable): String {
        val gitChanges = if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""
        val soc = if (Build.VERSION.SDK_INT >= 31) (Build.SOC_MANUFACTURER + ' ' + Build.SOC_MODEL) else "Unknown"

        val header = """
            Aliucord Manager v${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} $gitChanges

            Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}
            Device: ${Build.MANUFACTURER} - ${Build.MODEL} (${Build.DEVICE})
            SOC: $soc
        """.trimIndent()

        return header + "\n\n" + Log.getStackTraceString(stacktrace)
    }
}
