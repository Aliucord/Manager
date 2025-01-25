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
import com.aliucord.manager.util.debounce
import kotlinx.coroutines.launch

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

    val packageNameIsDefault by derivedStateOf {
        packageName == PatchOptions.Default.packageName
    }

    fun changePackageName(newPackageName: String) {
        packageName = newPackageName
        fetchPkgNameStateDebounced()
    }

    fun resetPackageName() {
        changePackageName(PatchOptions.Default.packageName)
    }

    // ---------- App name state ----------
    var appName by mutableStateOf(prefilledOptions.appName)
        private set

    var appNameIsError by mutableStateOf(false)
        private set

    val appNameIsDefault by derivedStateOf {
        appName == PatchOptions.Default.appName
    }

    fun changeAppName(newAppName: String) {
        appName = newAppName
        appNameIsError = newAppName.length !in (1..150)
    }

    fun resetAppName() {
        appName = PatchOptions.Default.appName
    }

    // ---------- Icon patching state ----------
    var replaceIcon by mutableStateOf(prefilledOptions.iconReplacement == PatchOptions.IconReplacement.Aliucord)
        private set

    fun changeReplaceIcon(value: Boolean) {
        replaceIcon = value
    }

    // ---------- Debuggable state ----------
    var debuggable by mutableStateOf(prefilledOptions.debuggable)
        private set

    fun changeDebuggable(value: Boolean) {
        debuggable = value
    }

    // ---------- Config generation ----------
    val isConfigValid by derivedStateOf {
        val invalidChecks = arrayOf(
            packageNameState == PackageNameState.Invalid,
            appNameIsError,
        )

        invalidChecks.none { it }
    }

    fun generateConfig(): PatchOptions {
        if (!isConfigValid) error("invalid config state")

        return PatchOptions(
            appName = appName,
            packageName = packageName,
            debuggable = debuggable,
            // TODO: advanced icon options
            iconReplacement = PatchOptions.IconReplacement.Aliucord,
            monochromeIcon = true,
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

    private fun fetchPkgNameState() {
        if (packageName.length !in (3..150) || !PACKAGE_REGEX.matches(this.packageName)) {
            packageNameState = PackageNameState.Invalid
        } else {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                packageNameState = PackageNameState.Taken
            } catch (_: NameNotFoundException) {
                packageNameState = PackageNameState.Ok
            }
        }
    }

    init {
        screenModelScope.launch { fetchPkgNameState() }
    }

    companion object {
        private val PACKAGE_REGEX = """^[a-z]\w*(\.[a-z]\w*)+$"""
            .toRegex(RegexOption.IGNORE_CASE)
    }
}

enum class PackageNameState {
    Ok,
    Invalid,
    Taken,
}
