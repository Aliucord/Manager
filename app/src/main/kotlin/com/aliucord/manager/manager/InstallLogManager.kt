package com.aliucord.manager.manager

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.util.IS_PROBABLY_EMULATOR
import com.aliucord.manager.util.isPlayProtectEnabled
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

/**
 * Central manager for storing all attempted installations and
 * their associated logs/crashes (not including manager crashes themselves).
 */
class InstallLogManager(
    private val application: Application,
    private val prefs: PreferencesManager,
    private val json: Json,
) {
    val logsDir = application.filesDir.resolve("install-logs").apply { mkdir() }

    /**
     * Loads the install log from disk, if it exists.
     */
    fun fetchInstallData(id: String): InstallLogData? {
        val path = logsDir.resolve("$id.json")
        if (!path.exists()) return null

        return try {
            @OptIn(ExperimentalSerializationApi::class)
            json.decodeFromStream(path.inputStream())
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to open install log $id", t)
            null
        }
    }

    /**
     * Writes an install log entry to disk.
     */
    suspend fun storeInstallData(
        id: String,
        installDate: Instant,
        installDuration: Duration,
        options: PatchOptions,
        log: String,
        error: Throwable?,
    ) {
        val path = logsDir.resolve("$id.json")

        val data = InstallLogData(
            id = id,
            installDate = installDate,
            installDuration = installDuration,
            installOptions = options,
            environmentInfo = getEnvironmentInfo(),
            installationLog = log,
            errorStacktrace = error?.let { Log.getStackTraceString(it).trimEnd() },
        )

        path.writeText(json.encodeToString(data))
    }

    /**
     * Creates a list of details about the current installation environment.
     */
    suspend fun getEnvironmentInfo(): String {
        @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
        val gitChanges = if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes present)" else ""
        val soc = if (Build.VERSION.SDK_INT >= 31) (Build.SOC_MANUFACTURER + ' ' + Build.SOC_MODEL) else "Unavailable"
        val playProtect = when (application.isPlayProtectEnabled()) {
            null -> "Unavailable"
            true -> "Enabled"
            false -> "Disabled"
        }

        return """
            Aliucord Manager v${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} $gitChanges
            Developer mode: ${if (prefs.devMode) "On" else "Off"}

            Android API: ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}
            ROM: Android ${Build.VERSION.RELEASE} (Patch ${Build.VERSION.SECURITY_PATCH})
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})
            Emulator: ${if (IS_PROBABLY_EMULATOR) "Yes" else "No"} (guess)
            Play Protect: $playProtect
            SOC: $soc
        """.trimIndent()
    }
}

@Immutable
@Serializable
data class InstallLogData(
    val id: String,
    val installDate: Instant,
    val installDuration: Duration,
    val installOptions: PatchOptions,
    val environmentInfo: String,
    val installationLog: String,
    val errorStacktrace: String?,
) {
    val isError: Boolean
        get() = errorStacktrace != null

    fun getFormattedInstallDate(): String {
        @SuppressLint("SimpleDateFormat")
        return SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.ENGLISH)
            .format(Date(installDate.toEpochMilliseconds()))
    }

    fun getLogFileContents(): String = buildString {
        appendLine("////////////////// Environment Info //////////////////")
        appendLine(environmentInfo)

        append("\n\n")
        appendLine("////////////////// Installation Info //////////////////")
        appendLine()
        append("Install ID: ")
        appendLine(id)
        append("Install time: ")
        appendLine(getFormattedInstallDate())
        append("Result: ")
        appendLine(if (isError) "Failure" else "Success")

        append("\n\n")
        appendLine("////////////////// Error Stacktrace //////////////////")
        appendLine()
        appendLine(errorStacktrace ?: "None")

        append("\n\n")
        appendLine("////////////////// Installation Log //////////////////")
        appendLine()
        appendLine(installationLog)
    }
}
