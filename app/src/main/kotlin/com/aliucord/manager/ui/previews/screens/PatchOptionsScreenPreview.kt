package com.aliucord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.patchopts.*

// This preview has scrollable/interactable content that cannot be tested from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun PatchOptionsScreenPreview(
    @PreviewParameter(PatchOptionsParametersProvider::class)
    parameters: PatchOptionsParameters,
) {
    ManagerTheme {
        PatchOptionsScreenContent(
            isUpdate = parameters.isUpdate,
            isDevMode = parameters.isDevMode,
            debuggable = parameters.debuggable,
            setDebuggable = {},
            onOpenIconOptions = {},
            appName = parameters.appName,
            appNameIsError = parameters.appNameIsError,
            setAppName = {},
            packageName = parameters.packageName,
            packageNameState = parameters.packageNameState,
            setPackageName = {},
            isConfigValid = parameters.isConfigValid,
            onInstall = {},
        )
    }
}

private data class PatchOptionsParameters(
    val isUpdate: Boolean,
    val isDevMode: Boolean,
    val debuggable: Boolean,
    val appName: String,
    val appNameIsError: Boolean,
    val packageName: String,
    val packageNameState: PackageNameState,
    val isConfigValid: Boolean,
)

private class PatchOptionsParametersProvider : PreviewParameterProvider<PatchOptionsParameters> {
    override val values = sequenceOf(
        // Default initial install
        PatchOptionsParameters(
            isUpdate = false,
            isDevMode = false,
            debuggable = false,
            appName = PatchOptions.Default.appName,
            appNameIsError = false,
            packageName = PatchOptions.Default.packageName,
            packageNameState = PackageNameState.Ok,
            isConfigValid = true,
        ),
        PatchOptionsParameters(
            isUpdate = true,
            isDevMode = false,
            debuggable = false,
            appName = "an invalid app name.",
            appNameIsError = true,
            packageName = "a b",
            packageNameState = PackageNameState.Invalid,
            isConfigValid = false,
        ),
        PatchOptionsParameters(
            isUpdate = false,
            isDevMode = true,
            debuggable = true,
            appName = PatchOptions.Default.appName,
            appNameIsError = false,
            packageName = PatchOptions.Default.packageName,
            packageNameState = PackageNameState.Taken,
            isConfigValid = true,
        ),
    )
}
