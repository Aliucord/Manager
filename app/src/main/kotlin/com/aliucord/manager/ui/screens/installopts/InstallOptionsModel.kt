package com.aliucord.manager.ui.screens.installopts

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.util.debounce

class InstallOptionsModel(
    private val context: Context,
) : ScreenModel {
    var packageName by mutableStateOf("com.aliucord")
        private set

    var packageNameState by mutableStateOf(PackageNameState.Ok)
        private set

    fun changePackageName(newPackageName: String) {
        packageName = newPackageName
        updatePackageNameState()
    }

    // Use debounce here instead
    private val updatePackageNameState: () -> Unit = screenModelScope.debounce(100L) {
        if (!PACKAGE_REGEX.matches(this.packageName)) {
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

    companion object {
        private val PACKAGE_REGEX = "^[a-z]\\w*(\\.[a-z]\\w*)+\$"
            .toRegex(RegexOption.IGNORE_CASE)
    }
}

enum class PackageNameState {
    Ok,
    Invalid,
    Taken,
}
