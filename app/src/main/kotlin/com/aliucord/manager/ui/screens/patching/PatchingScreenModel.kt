package com.aliucord.manager.ui.screens.patching

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.*
import com.aliucord.manager.patcher.KotlinPatchRunner
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.*
import com.aliucord.manager.patcher.steps.install.InstallStep
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.util.toUnsafeImmutable
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PatchingScreenModel(
    private val options: PatchOptions,
    private val paths: PathManager,
    private val prefs: PreferencesManager,
    private val application: Application,
    private val installLogs: InstallLogManager,
) : StateScreenModel<PatchingScreenState>(PatchingScreenState.Working) {
    private var installId: String? = null
    private var startTime: Instant? = null
    private var runnerJob: Job? = null
    private var stepRunner: StepRunner? = null

    val devMode get() = prefs.devMode

    var steps by mutableStateOf<ImmutableMap<StepGroup, ImmutableList<Step>>?>(null)
        private set

    @get:StringRes
    var funFact by mutableIntStateOf(0)
        private set

    init {
        install()

        // Rotate fun facts every so often
        screenModelScope.launch {
            while (true) {
                funFact = FUN_FACTS.random()
                delay(8.seconds)
            }
        }
    }

    fun getCurrentInstallId(): String? = installId

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

    fun cancelInstall() = screenModelScope.launchBlock(Dispatchers.IO) {
        runnerJob?.cancel("Manual cancellation")

        val incompleteDownloadStep = stepRunner?.steps
            ?.filterIsInstance<DownloadStep>()
            ?.lastOrNull { it.state == StepState.Running }

        incompleteDownloadStep?.targetFile?.delete()
        paths.patchingWorkingDir().deleteRecursively()
    }

    fun install() = screenModelScope.launch {
        runnerJob?.cancel("Manual cancellation")
        steps = null

        @SuppressLint("MemberExtensionConflict")
        installId = UUID.randomUUID().toString()
        startTime = Clock.System.now()
        mutableState.value = PatchingScreenState.Working

        runnerJob = screenModelScope.launch {
            Log.i(BuildConfig.TAG, "Starting installation with environment:\n" + installLogs.getEnvironmentInfo())

            try {
                startPatchRunner()
            } catch (_: CancellationException) {
                Log.w(BuildConfig.TAG, "Installation was cancelled before completion")
                mutableState.value = PatchingScreenState.CloseScreen
            } catch (error: Throwable) {
                Log.e(BuildConfig.TAG, "Failed to orchestrate patch runner", error)
                mutableState.value = PatchingScreenState.Failed(installId = installId!!)
                installLogs.storeInstallData(
                    id = installId!!,
                    installDate = startTime!!,
                    installDuration = Duration.ZERO,
                    options = options,
                    log = "- Failed to initialize patch runner",
                    error = error,
                )
            }
        }
    }

    private suspend fun startPatchRunner() {
        val runner = KotlinPatchRunner(options)
            .also { stepRunner = it }

        steps = runner.steps.groupBy { it.group }
            .mapValues { it.value.toUnsafeImmutable() }
            .toUnsafeImmutable()

        // Intentionally delay to show the state change of the first step when it runs in the UI.
        // Without this, on a fast internet connection the step just immediately shows as "Success".
        delay(400)

        // Execute all the steps and catch any errors
        val error = when (val error = runner.executeAll()) {
            null -> {
                // If install step is marked skipped then the installation was manually aborted
                // and if so, immediately close install screen
                if (runner.getStep<InstallStep>().state == StepState.Skipped) {
                    mutableState.value = PatchingScreenState.CloseScreen

                    Error("Installation was aborted or cancelled")
                        .apply { stackTrace = emptyArray() }
                }
                // At this point, the installation has successfully completed
                else {
                    mutableState.value = PatchingScreenState.Success

                    null
                }
            }

            else -> {
                Log.e(BuildConfig.TAG, "Failed to perform installation process", error)
                mutableState.value = PatchingScreenState.Failed(installId = installId!!)

                error
            }
        }

        installLogs.storeInstallData(
            id = installId!!,
            installDate = startTime!!,
            installDuration = runner.steps.sumOf { it.getDuration() }.milliseconds,
            options = options,
            log = runner.getLog(),
            error = error,
        )
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
