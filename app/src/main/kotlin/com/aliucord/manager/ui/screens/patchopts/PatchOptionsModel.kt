package com.aliucord.manager.ui.screens.patchopts

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import androidx.compose.runtime.*
import androidx.core.content.getSystemService
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.util.*

class PatchOptionsModel(
    prefilledOptions: PatchOptions,
    private val context: Context,
    private val prefs: PreferencesManager,
) : ScreenModel {
    // ---------- Package name state ----------
    var packageName by mutableStateOf(prefilledOptions.packageName)
        private set

    var packageNameState by mutableStateOf(PackageNameState.Ok)
        private set

    fun changePackageName(newPackageName: String) {
        packageName = newPackageName
        fetchPkgNameStateDebounced()
    }

    // ---------- App name state ----------
    var appName by mutableStateOf(prefilledOptions.appName)
        private set

    var appNameIsError by mutableStateOf(false)
        private set

    fun changeAppName(newAppName: String) {
        appName = newAppName
        appNameIsError = newAppName.length !in (1..150)
    }

    // ---------- Debuggable state ----------
    var debuggable by mutableStateOf(prefilledOptions.debuggable)
        private set

    fun changeDebuggable(value: Boolean) {
        debuggable = value
    }

    // ---------- Other ----------
    var showNetworkWarningDialog by mutableStateOf(!alreadyShownNetworkWarning && isNetworkDangerous())
        private set

    fun hideNetworkWarning(neverShow: Boolean) {
        showNetworkWarningDialog = false
        alreadyShownNetworkWarning = true
        prefs.showNetworkWarning = !neverShow
    }

    // ---------- Config generation ----------
    val isConfigValid by derivedStateOf {
        val invalidChecks = arrayOf(
            packageNameState == PackageNameState.Invalid,
            appNameIsError,
        )

        invalidChecks.none { it }
    }

    fun generateConfig(icon: PatchOptions.IconReplacement): PatchOptions {
        if (!isConfigValid) error("invalid config state")

        return PatchOptions(
            appName = appName,
            packageName = packageName,
            debuggable = debuggable,
            iconReplacement = icon,
        )
    }

    // ---------- Other ----------
    val isDevMode: Boolean
        get() = prefs.devMode

    /**
     * Check whether the device is connected on a metered WIFI connection or through any type of mobile data,
     * to avoid unknowingly downloading a lot of stuff through a potentially metered network.
     */
    @Suppress("DEPRECATION")
    fun isNetworkDangerous(): Boolean {
        val connectivity = context.getSystemService<ConnectivityManager>()
            ?: error("Unable to get system connectivity service")

        if (connectivity.isActiveNetworkMetered) return true

        when (val info = connectivity.activeNetworkInfo) {
            null -> return false
            else -> {
                if (info.isRoaming) return true
                if (info.type == ConnectivityManager.TYPE_WIFI) return false
            }
        }

        val telephony = context.getSystemService<TelephonyManager>()
            ?: error("Unable to get system telephony service")

        val dangerousMobileDataStates = arrayOf(
            /* TelephonyManager.DATA_DISCONNECTING */ 4,
            TelephonyManager.DATA_CONNECTED,
            TelephonyManager.DATA_CONNECTING,
        )

        return dangerousMobileDataStates.contains(telephony.dataState)
    }

    // A throttled variant of fetchPkgNameState()
    private val fetchPkgNameStateDebounced: () -> Unit =
        screenModelScope.debounce(100L, function = ::fetchPkgNameState)

    private suspend fun fetchPkgNameState() {
        val state = if (packageName.length !in (3..150) || !PACKAGE_REGEX.matches(this.packageName)) {
            PackageNameState.Invalid
        } else {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                PackageNameState.Taken
            } catch (_: NameNotFoundException) {
                PackageNameState.Ok
            }
        }

        mainThread { packageNameState = state }
    }

    init {
        if (!prefs.showNetworkWarning)
            showNetworkWarningDialog = false

        screenModelScope.launchBlock { fetchPkgNameState() }
    }

    companion object {
        // Global state to avoid showing the warning more than once per launch
        private var alreadyShownNetworkWarning = false

        private val PACKAGE_REGEX = """^[a-z]\w*(\.[a-z]\w*)+$"""
            .toRegex(RegexOption.IGNORE_CASE)
    }
}

enum class PackageNameState {
    Ok,
    Invalid,
    Taken,
}
