package com.aliucord.manager.ui.screens.installopts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.TextDivider
import com.aliucord.manager.ui.screens.install.InstallScreen
import com.aliucord.manager.ui.screens.installopts.components.InstallOptionsAppBar
import com.aliucord.manager.ui.screens.installopts.components.PackageNameState
import com.aliucord.manager.ui.screens.installopts.components.options.SwitchInstallOption
import com.aliucord.manager.ui.screens.installopts.components.options.TextInstallOption
import com.aliucord.manager.ui.util.thenIf

class InstallOptionsScreen : Screen {
    override val key = "InstallOptions"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = getScreenModel<InstallOptionsModel>()

        Scaffold(
            topBar = { InstallOptionsAppBar() },
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.installopts_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                TextDivider(
                    text = "Basic",
                    modifier = Modifier.padding(top = 8.dp),
                )

                SwitchInstallOption(
                    icon = painterResource(R.drawable.ic_app_shortcut),
                    name = stringResource(R.string.installopts_icon_title),
                    description = stringResource(R.string.installopts_icon_desc),
                    value = model.replaceIcon,
                    onValueChange = model::changeReplaceIcon,
                )

                TextInstallOption(
                    name = stringResource(R.string.installopts_appname_title),
                    description = stringResource(R.string.installopts_appname_desc),
                    value = model.appName,
                    valueIsError = model.appNameIsError,
                    valueIsDefault = model.appNameIsDefault,
                    onValueChange = model::changeAppName,
                    onValueReset = model::resetAppName
                )

                TextInstallOption(
                    name = stringResource(R.string.installopts_pkgname_title),
                    description = stringResource(R.string.installopts_pkgname_desc),
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

                TextDivider(
                    text = "Advanced",
                    modifier = Modifier.padding(top = 8.dp),
                )

                SwitchInstallOption(
                    icon = painterResource(R.drawable.ic_bug),
                    name = stringResource(R.string.installopts_debuggable_title),
                    description = stringResource(R.string.installopts_debuggable_desc),
                    value = model.debuggable,
                    onValueChange = model::changeDebuggable,
                    enabled = model.isDevMode,
                    modifier = Modifier
                        .thenIf(!model.isDevMode) { alpha(.6f) }
                )

                Spacer(Modifier.weight(1f))

                FilledTonalButton(
                    enabled = model.isConfigValid,
                    onClick = { navigator replace InstallScreen(model.generateConfig()) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_install))
                }
            }
        }
    }
}
