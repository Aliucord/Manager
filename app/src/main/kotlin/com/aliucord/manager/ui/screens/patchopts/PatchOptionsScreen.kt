package com.aliucord.manager.ui.screens.patchopts

import android.os.Parcelable
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.TextDivider
import com.aliucord.manager.ui.components.dialogs.NetworkWarningDialog
import com.aliucord.manager.ui.components.dialogs.UnknownSourcesPermissionDialog
import com.aliucord.manager.ui.screens.iconopts.IconOptionsScreen
import com.aliucord.manager.ui.screens.patching.PatchingScreen
import com.aliucord.manager.ui.screens.patchopts.components.PackageNameStateLabel
import com.aliucord.manager.ui.screens.patchopts.components.PatchOptionsAppBar
import com.aliucord.manager.ui.screens.patchopts.components.options.*
import com.aliucord.manager.ui.util.InstallNotifications
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import com.aliucord.manager.util.isIgnoringBatteryOptimizations
import com.aliucord.manager.util.requestNoBatteryOptimizations
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Parcelize
class PatchOptionsScreen(
    private val prefilledOptions: PatchOptions? = null,
) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "PatchOptions"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<PatchOptionsModel> { parametersOf(prefilledOptions ?: PatchOptions.Default) }

        LaunchedEffect(Unit) {
            // Ensure that when popping this screen off the stack that permission requests don't get triggered
            // The coroutine context local to this screen gets cancelled and never continues after the delay()
            delay(1000)

            InstallNotifications.requestPermissions(context)

            if (!context.isIgnoringBatteryOptimizations())
                context.requestNoBatteryOptimizations()
        }

        var showNetworkWarningDialog by rememberSaveable { mutableStateOf(model.isNetworkDangerous()) }
        if (showNetworkWarningDialog) {
            NetworkWarningDialog(
                onConfirm = { showNetworkWarningDialog = false },
                onDismiss = {
                    showNetworkWarningDialog = false
                    navigator.pop()
                },
            )
        }

        UnknownSourcesPermissionDialog()

        PatchOptionsScreenContent(
            isUpdate = prefilledOptions != null,
            isDevMode = model.isDevMode,

            debuggable = model.debuggable,
            setDebuggable = model::changeDebuggable,

            // TODO: actual filled in options
            onOpenIconOptions = { navigator.push(IconOptionsScreen(PatchOptions.Default.iconReplacement)) },

            appName = model.appName,
            appNameIsError = model.appNameIsError,
            setAppName = model::changeAppName,

            packageName = model.packageName,
            packageNameState = model.packageNameState,
            setPackageName = model::changePackageName,

            isConfigValid = model.isConfigValid,
            onInstall = { navigator.push(PatchingScreen(model.generateConfig())) },
        )
    }
}

@Composable
fun PatchOptionsScreenContent(
    isUpdate: Boolean,
    isDevMode: Boolean,

    debuggable: Boolean,
    setDebuggable: (Boolean) -> Unit,

    onOpenIconOptions: () -> Unit,

    appName: String,
    appNameIsError: Boolean,
    setAppName: (String) -> Unit,

    packageName: String,
    packageNameState: PackageNameState,
    setPackageName: (String) -> Unit,

    isConfigValid: Boolean,
    onInstall: () -> Unit,
) {
    Scaffold(
        topBar = { PatchOptionsAppBar(isUpdate = isUpdate) },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedByLastAtBottom(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.patchopts_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            TextDivider(text = stringResource(R.string.patchopts_divider_basic))

            IconPatchOption(
                icon = painterResource(R.drawable.ic_app_shortcut),
                name = stringResource(R.string.patchopts_icon_title),
                description = stringResource(R.string.patchopts_icon_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember(::MutableInteractionSource),
                        onClick = onOpenIconOptions,
                        indication = null,
                        role = Role.Switch,
                    )
                    .padding(bottom = 8.dp),
            ) {
                // TODO: selected icon options preview
                // Icon(
                //     painter = painterResource(R.drawable.ic_discord),
                //     contentDescription = null,
                //     modifier = Modifier
                //         .padding(start = 4.dp, end = 14.dp)
                //         .size(26.dp),
                // )
            }

            val appNameIsDefault by remember {
                derivedStateOf {
                    appName == PatchOptions.Default.appName
                }
            }
            TextPatchOption(
                name = stringResource(R.string.patchopts_appname_title),
                description = stringResource(R.string.patchopts_appname_desc),
                value = appName,
                valueIsError = appNameIsError,
                valueIsDefault = appNameIsDefault,
                onValueChange = setAppName,
                onValueReset = { setAppName(PatchOptions.Default.appName) },
            )

            val packageNameIsDefault by remember {
                derivedStateOf {
                    packageName == PatchOptions.Default.packageName
                }
            }
            TextPatchOption(
                name = stringResource(R.string.patchopts_pkgname_title),
                description = stringResource(R.string.patchopts_pkgname_desc),
                value = packageName,
                valueIsError = packageNameState == PackageNameState.Invalid,
                valueIsDefault = packageNameIsDefault,
                onValueChange = setPackageName,
                onValueReset = { setPackageName(PatchOptions.Default.packageName) },
            ) {
                PackageNameStateLabel(
                    state = packageNameState,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (isDevMode) {
                TextDivider(
                    text = stringResource(R.string.patchopts_divider_advanced),
                    modifier = Modifier.padding(top = 12.dp),
                )

                SwitchPatchOption(
                    icon = painterResource(R.drawable.ic_bug),
                    name = stringResource(R.string.patchopts_debuggable_title),
                    description = stringResource(R.string.patchopts_debuggable_desc),
                    value = debuggable,
                    onValueChange = setDebuggable,
                )
            }

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                enabled = isConfigValid,
                onClick = onInstall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .align(Alignment.End),
            ) {
                Text(stringResource(R.string.action_install))
            }
        }
    }
}
