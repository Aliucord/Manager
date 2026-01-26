package com.aliucord.manager.ui.screens.patchopts

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.Navigator
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.screens.componentopts.ComponentOptionsScreen
import com.aliucord.manager.ui.screens.componentopts.PatchComponent
import com.aliucord.manager.ui.util.pushForResult
import com.aliucord.manager.util.*
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

    // ---------- Custom components state ----------
    var customInjector by mutableStateOf<PatchComponent?>(null)
        private set
    var customPatches by mutableStateOf<PatchComponent?>(null)
        private set

    fun selectCustomInjector(navigator: Navigator) = screenModelScope.launch {
        customInjector = navigator.pushForResult(
            ComponentOptionsScreen(
                default = customInjector,
                componentType = PatchComponent.Type.Injector,
            )
        )
    }

    fun selectCustomPatches(navigator: Navigator) = screenModelScope.launch {
        customPatches = navigator.pushForResult(
            ComponentOptionsScreen(
                default = customPatches,
                componentType = PatchComponent.Type.Patches,
            )
        )
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
            customInjector = customInjector,
            customPatches = customPatches,
        )
    }

    // ---------- Other ----------
    val isDevMode: Boolean
        get() = prefs.devMode

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
        screenModelScope.launchBlock { fetchPkgNameState() }
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
