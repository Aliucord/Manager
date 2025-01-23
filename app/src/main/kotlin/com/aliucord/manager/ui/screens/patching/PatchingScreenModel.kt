package com.aliucord.manager.ui.screens.patching

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.patcher.KotlinPatchRunner
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.install.InstallStep
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration.Companion.seconds

class PatchingScreenModel(
    private val application: Application,
    private val paths: PathManager,
    private val options: PatchOptions,
    private val prefs: PreferencesManager,
) : StateScreenModel<PatchingScreenState>(PatchingScreenState.Working) {
    private var startTime: Date? = null
    private var runnerJob: Job? = null
    private var stepRunner: StepRunner? = null

    val devMode get() = prefs.devMode

    var steps by mutableStateOf<ImmutableMap<StepGroup, ImmutableList<Step>>?>(null)
        private set

    @get:StringRes
    var funFact by mutableIntStateOf(0)
        private set

    init {
        restart()

        // Rotate fun facts every so often
        screenModelScope.launch {
            while (true) {
                funFact = FUN_FACTS.random()
                delay(8.seconds)
            }
        }
    }

    fun launchApp() {
        if (state.value !is PatchingScreenState.Success)
            return

        val launchIntent = application.packageManager
            .getLaunchIntentForPackage(options.packageName)

        if (launchIntent != null) {
            application.startActivity(launchIntent)
        } else {
            application.showToast(R.string.launch_aliucord_fail)
        }
    }

    fun copyDebugToClipboard() {
        val content = (state.value as? PatchingScreenState.Failed)?.failureLog
            ?: return

        application.copyToClipboard(content)
        application.showToast(R.string.action_copied)
    }

    fun saveFailureLog() {
        val startTime = startTime ?: return
        val failureLog = (state.value as? PatchingScreenState.Failed)?.failureLog
            ?: return

        @SuppressLint("SimpleDateFormat")
        val formattedDate = SimpleDateFormat("yyyy-MM-dd hh-mm-s a").format(startTime)
        val fileName = "Aliucord Manager $formattedDate.log"

        application.saveFile(fileName, failureLog)
    }

    fun clearCache() {
        screenModelScope.launch { paths.clearCache() }
        application.showToast(R.string.action_cleared_cache)
    }

    fun restart() {
        runnerJob?.cancel("Manual cancellation")
        steps = null

        startTime = Date()
        mutableState.value = PatchingScreenState.Working

        runnerJob = screenModelScope.launch {
            try {
                startPatchRunner()
            } catch (_: CancellationException) {
                Log.w(BuildConfig.TAG, "Installation was cancelled before completion")
                mutableState.value = PatchingScreenState.CloseScreen
            } catch (error: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to orchestrate patch runner", error)
                mutableState.value = PatchingScreenState.Failed(failureLog = getFailureInfo(error))
            }
        }
    }

    private suspend fun startPatchRunner() {
        val runner = KotlinPatchRunner(options)
            .also { stepRunner = it }

        steps = runner.steps.groupBy { it.group }
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
                    mutableState.value = PatchingScreenState.CloseScreen
                }
                // At this point, the installation has successfully completed
                else {
                    mutableState.value = PatchingScreenState.Success
                }
            }

            else -> {
                Log.e(BuildConfig.TAG, "Failed to perform installation process", error)
                mutableState.value = PatchingScreenState.Failed(failureLog = getFailureInfo(error))
            }
        }
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
            Emulator: $IS_PROBABLY_EMULATOR (guess)
            ROM: Android ${Build.VERSION.RELEASE} (Patch ${if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "<none>"})
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})
            Play Protect: $playProtect
            SOC: $soc
        """.trimIndent()

        return header + "\n\n" + Log.getStackTraceString(stacktrace).trimEnd()
    }

    private companion object {
        /**
         * Random fun facts to show on the installation screen.
         */
        val FUN_FACTS = arrayOf(
            R.string.fun_fact_1,
            R.string.fun_fact_2,
            R.string.fun_fact_3,
            R.string.fun_fact_4,
            R.string.fun_fact_5,
            R.string.fun_fact_6,
            R.string.fun_fact_7,
            R.string.fun_fact_8,
            R.string.fun_fact_9,
            R.string.fun_fact_10,
            R.string.fun_fact_11,
            R.string.fun_fact_12,
        )
    }
}
