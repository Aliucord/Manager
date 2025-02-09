package com.aliucord.manager.ui.screens.patchopts

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.AnimatedVersionDisplay
import com.aliucord.manager.ui.components.TextDivider
import com.aliucord.manager.ui.components.dialogs.NetworkWarningDialog
import com.aliucord.manager.ui.components.dialogs.UnknownSourcesPermissionDialog
import com.aliucord.manager.ui.screens.patching.PatchingScreen
import com.aliucord.manager.ui.screens.patchopts.components.PackageNameState
import com.aliucord.manager.ui.screens.patchopts.components.PatchOptionsAppBar
import com.aliucord.manager.ui.screens.patchopts.components.options.SwitchPatchOption
import com.aliucord.manager.ui.screens.patchopts.components.options.TextPatchOption
import com.aliucord.manager.ui.util.*
import com.aliucord.manager.util.isIgnoringBatteryOptimizations
import com.aliucord.manager.util.requestNoBatteryOptimizations
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Parcelize
class PatchOptionsScreen(
    private val prefilledOptions: PatchOptions? = null,
    private val supportedVersion: DiscordVersion = DiscordVersion.None,
) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "PatchOptions"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<PatchOptionsModel>() { parametersOf(prefilledOptions ?: PatchOptions.Default) }

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

        Scaffold(
            topBar = { PatchOptionsAppBar(isUpdate = prefilledOptions != null) },
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

                var animatedVersion by remember { mutableStateOf<DiscordVersion>(DiscordVersion.None) }
                LaunchedEffect(Unit) {
                    animatedVersion = supportedVersion
                }
                AnimatedVersionDisplay(
                    version = animatedVersion,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = (-10).dp),
                )

                TextDivider(text = stringResource(R.string.patchopts_divider_basic))

                SwitchPatchOption(
                    icon = painterResource(R.drawable.ic_app_shortcut),
                    name = stringResource(R.string.patchopts_icon_title),
                    description = stringResource(R.string.patchopts_icon_desc),
                    value = model.replaceIcon,
                    onValueChange = model::changeReplaceIcon,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextPatchOption(
                    name = stringResource(R.string.patchopts_appname_title),
                    description = stringResource(R.string.patchopts_appname_desc),
                    value = model.appName,
                    valueIsError = model.appNameIsError,
                    valueIsDefault = model.appNameIsDefault,
                    onValueChange = model::changeAppName,
                    onValueReset = model::resetAppName
                )

                TextPatchOption(
                    name = stringResource(R.string.patchopts_pkgname_title),
                    description = stringResource(R.string.patchopts_pkgname_desc),
                    value = model.packageName,
                    valueIsError = model.packageNameState == PackageNameState.Invalid,
                    valueIsDefault = model.packageNameIsDefault,
                    onValueChange = model::changePackageName,
                    onValueReset = model::resetPackageName,
                ) {
                    PackageNameState(
                        state = model.packageNameState,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                if (model.isDevMode) {
                    TextDivider(
                        text = stringResource(R.string.patchopts_divider_advanced),
                        modifier = Modifier.padding(top = 12.dp),
                    )

                    SwitchPatchOption(
                        icon = painterResource(R.drawable.ic_bug),
                        name = stringResource(R.string.patchopts_debuggable_title),
                        description = stringResource(R.string.patchopts_debuggable_desc),
                        value = model.debuggable,
                        onValueChange = model::changeDebuggable,
                    )
                }

                Spacer(Modifier.weight(1f))

                FilledTonalButton(
                    enabled = model.isConfigValid,
                    onClick = { navigator.push(PatchingScreen(model.generateConfig())) },
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
}
