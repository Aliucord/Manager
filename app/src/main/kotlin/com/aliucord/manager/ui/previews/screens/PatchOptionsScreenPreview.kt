package com.aliucord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.patchopts.*
import com.aliucord.manager.ui.util.DiscordVersion

// This preview has scrollable/interactable content that cannot be tested from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun PatchOptionsScreenPreview(
    @PreviewParameter(ContributorsProvider::class)
    parameters: PatchOptionsParameters,
) {
    ManagerTheme {
        PatchOptionsScreenContent(
            isUpdate = parameters.isUpdate,
            isDevMode = parameters.isDevMode,
            supportedVersion = parameters.supportedVersion,
            debuggable = parameters.debuggable,
            setDebuggable = {},
            replaceIcon = parameters.replaceIcon,
            setReplaceIcon = {},
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
    val supportedVersion: DiscordVersion,
    val debuggable: Boolean,
    val replaceIcon: Boolean,
    val appName: String,
    val appNameIsError: Boolean,
    val packageName: String,
    val packageNameState: PackageNameState,
    val isConfigValid: Boolean,
)

private class ContributorsProvider : PreviewParameterProvider<PatchOptionsParameters> {
    private val stableVersion = DiscordVersion.Existing(DiscordVersion.Type.STABLE, "126.21", 126021)

    override val values = sequenceOf(
        // Default initial install
        PatchOptionsParameters(
            isUpdate = false,
            isDevMode = false,
            supportedVersion = stableVersion,
            debuggable = false,
            replaceIcon = true,
            appName = PatchOptions.Default.appName,
            appNameIsError = false,
            packageName = PatchOptions.Default.packageName,
            packageNameState = PackageNameState.Ok,
            isConfigValid = true,
        ),
        PatchOptionsParameters(
            isUpdate = true,
            isDevMode = false,
            supportedVersion = stableVersion,
            debuggable = false,
            replaceIcon = true,
            appName = "an invalid app name.",
            appNameIsError = true,
            packageName = "a b",
            packageNameState = PackageNameState.Invalid,
            isConfigValid = false,
        ),
        PatchOptionsParameters(
            isUpdate = false,
            isDevMode = true,
            supportedVersion = stableVersion,
            debuggable = true,
            replaceIcon = false,
            appName = PatchOptions.Default.appName,
            appNameIsError = false,
            packageName = PatchOptions.Default.packageName,
            packageNameState = PackageNameState.Taken,
            isConfigValid = true,
        ),
    )
}
