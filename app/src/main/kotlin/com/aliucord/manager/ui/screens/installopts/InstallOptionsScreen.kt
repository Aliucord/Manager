package com.aliucord.manager.ui.screens.installopts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.components.TextDivider
import com.aliucord.manager.ui.components.settings.SettingsSwitch
import com.aliucord.manager.ui.screens.install.InstallScreen
import com.aliucord.manager.ui.screens.installopts.components.AppNameSetting
import com.aliucord.manager.ui.screens.installopts.components.PackageNameSetting
import com.aliucord.manager.ui.screens.settings.SettingsScreen

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
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)
            ) {
                TextDivider(
                    text = "Basic",
                    modifier = Modifier.padding(top = 8.dp),
                )

                AppNameSetting(
                    name = model.appName,
                    nameIsDefault = model.appNameIsDefault,
                    nameIsError = model.appNameIsError,
                    onChangeName = model::changeAppName,
                    onResetName = model::resetAppName,
                )

                PackageNameSetting(
                    name = model.packageName,
                    nameIsDefault = model.packageNameIsDefault,
                    state = model.packageNameState,
                    onChangeName = model::changePackageName,
                    onResetName = model::resetPackageName,
                )

                SettingsSwitch(
                    label = stringResource(R.string.setting_replace_icon),
                    secondaryLabel = stringResource(R.string.setting_replace_icon_desc),
                    icon = { Icon(painterResource(R.drawable.ic_app_shortcut), null) },
                    pref = model.replaceIcon,
                    onPrefChange = model::changeReplaceIcon,
                )

                TextDivider(
                    text = "Advanced",
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsSwitch(
                    label = stringResource(R.string.setting_debuggable),
                    secondaryLabel = stringResource(R.string.setting_debuggable_desc),
                    icon = { Icon(painterResource(R.drawable.ic_bug), null) },
                    pref = model.debuggable,
                    onPrefChange = model::changeDebuggable,
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

@Composable
fun InstallOptionsAppBar() {
    TopAppBar(
        navigationIcon = { BackButton() },
        title = { Text("Configure installation") },
        actions = {
            val navigator = LocalNavigator.currentOrThrow

            IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.navigation_settings)
                )
            }
        }
    )
}
