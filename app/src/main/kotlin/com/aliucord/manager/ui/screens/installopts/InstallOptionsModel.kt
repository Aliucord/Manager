package com.aliucord.manager.ui.screens.installopts

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.util.debounce
import kotlinx.coroutines.launch

class InstallOptionsModel(
    private val context: Context,
    private val prefs: PreferencesManager,
) : ScreenModel {
    // ---------- Package name state ----------
    var packageName by mutableStateOf("com.aliucord")
        private set

    var packageNameState by mutableStateOf(PackageNameState.Ok)
        private set

    val packageNameIsDefault by derivedStateOf {
        packageName == "com.aliucord"
    }

    fun changePackageName(newPackageName: String) {
        packageName = newPackageName
        fetchPkgNameStateDebounced()
    }

    fun resetPackageName() {
        changePackageName("com.aliucord")
    }

    // ---------- App name state ----------
    var appName by mutableStateOf("Aliucord")
        private set

    var appNameIsError by mutableStateOf(false)
        private set

    val appNameIsDefault by derivedStateOf {
        appName == "Aliucord"
    }

    fun changeAppName(newAppName: String) {
        appName = newAppName
        appNameIsError = newAppName.length !in (1..150)
    }

    fun resetAppName() {
        appName = "Aliucord"
    }

    // ---------- Icon patching state ----------
    var replaceIcon by mutableStateOf(true)
        private set

    fun changeReplaceIcon(value: Boolean) {
        replaceIcon = value
    }

    // ---------- Debuggable state ----------
    var debuggable by mutableStateOf(false)
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

    fun generateConfig(): InstallOptions {
        if (!isConfigValid) error("invalid config state")

        return InstallOptions(
            appName = appName,
            packageName = packageName,
            debuggable = debuggable,
            // TODO: advanced icon options
            iconReplacement = InstallOptions.IconReplacement.Aliucord,
            monochromeIcon = true,
        )
    }

    // ---------- Other ----------
    val isDevMode: Boolean
        get() = prefs.devMode

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
