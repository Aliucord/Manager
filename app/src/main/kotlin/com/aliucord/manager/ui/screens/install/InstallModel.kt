@file:OptIn(ExperimentalCoroutinesApi::class)

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
import com.aliucord.manager.installer.steps.*
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.installer.steps.install.InstallStep
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date

class InstallModel(
    private val application: Application,
    private val paths: PathManager,
    private val options: InstallOptions,
) : StateScreenModel<InstallScreenState>(InstallScreenState.Pending) {
    private var startTime: Date? = null
    private var installJob: Job? = null
    private var stepRunner: StepRunner? = null

    private var autocloseCancelled: Boolean = false

    var installSteps by mutableStateOf<ImmutableMap<StepGroup, ImmutableList<Step>>?>(null)
        private set

    var showGppWarning by mutableStateOf(false)
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
        val startTime = startTime ?: return
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

    /**
     * Cancel the screen auto-close once installation was completed
     */
    fun cancelAutoclose() {
        autocloseCancelled = true
    }

    /**
     * Hide the 'Google Play Protect is enabled on your device' warning dialog
     */
    fun dismissGPPWarning() {
        showGppWarning = false

        // Continue executing step
        screenModelScope.launch {
            stepRunner
                ?.getStep<InstallStep>(completed = false)
                ?.dismissGPPWarning()
        }
    }

    fun restart() {
        installJob?.cancel("Manual cancellation")
        installSteps = null

        startTime = Date()
        mutableState.value = InstallScreenState.Working

        val newInstallJob = screenModelScope.launch {
            val runner = KotlinInstallRunner(options)
                .also { stepRunner = it }

            // Bind InstallStep's GPP Warning state to this
            runner.getStep<InstallStep>(completed = false)
                .gppWarningLock
                .take(1) // Take only the initially trigger value
                .onEach { showGppWarning = true }
                .launchIn(this@launch)

            installSteps = runner.steps.groupBy { it.group }
                .mapValues { it.value.toUnsafeImmutable() }
                .toUnsafeImmutable()

            // Intentionally delay to show the state change of the first step in UI when it runs
            // without it, on a fast internet it just immediately shows as "Success"
            delay(600)

            // Execute all the steps and catch any errors
            when (val error = runner.executeAll()) {
                null -> {
                    // If install step is marked skipped then the installation was manually aborted
                    // and if so, immediately close install screen
                    if (runner.getStep<InstallStep>().state == StepState.Skipped) {
                        mutableState.value = InstallScreenState.CloseScreen
                    }
                    // At this point, the installation has successfully completed
                    else {
                        mutableState.value = InstallScreenState.Success
                        autocloseCancelled = false

                        // Wait 5s before returning to Home if screen hasn't been clicked
                        delay(5000)
                        if (!autocloseCancelled) {
                            mutableState.value = InstallScreenState.CloseScreen
                        }
                    }
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

    private suspend fun getFailureInfo(stacktrace: Throwable): String {
        val gitChanges = if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""
        val soc = if (Build.VERSION.SDK_INT >= 31) (Build.SOC_MANUFACTURER + ' ' + Build.SOC_MODEL) else "Unavailable"
        val playProtect = when (application.isPlayProtectEnabled()) {
            null -> "Unavailable"
            true -> "Enabled"
            false -> "Disabled"
        }

        val header = """
            Aliucord Manager v${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} $gitChanges

            Android API: ${Build.VERSION.SDK_INT}
            ROM: Android ${Build.VERSION.RELEASE} (Patch ${Build.VERSION.SECURITY_PATCH})
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})
            Play Protect: $playProtect
            SOC: $soc
        """.trimIndent()

        return header + "\n\n" + Log.getStackTraceString(stacktrace)
    }
}
