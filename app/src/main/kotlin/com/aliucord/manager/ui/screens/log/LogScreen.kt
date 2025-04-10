package com.aliucord.manager.ui.screens.log

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.manager.InstallLogData
import com.aliucord.manager.ui.components.back
import com.aliucord.manager.ui.screens.log.components.LogAppBar
import com.aliucord.manager.ui.screens.log.components.SelectableTextArea
import com.aliucord.manager.ui.screens.patchopts.components.options.PatchOption
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Parcelize
class LogScreen(private val installId: String) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key: ScreenKey
        get() = "LogScreen-$installId"

    @Composable
    override fun Content() {
        val model = getScreenModel<LogScreenModel> { parametersOf(installId) }
        val navigator = LocalNavigator.currentOrThrow

        if (model.shouldCloseScreen) {
            navigator.back(currentActivity = null)
        }

        model.data?.let {
            LogScreenContent(
                data = it,
                onExportLog = model::saveLog,
                onShareLog = model::shareLog,
            )
        }
    }
}

@Composable
fun LogScreenContent(
    data: InstallLogData,
    onExportLog: () -> Unit,
    onShareLog: () -> Unit,
) {
    Scaffold(
        topBar = {
            LogAppBar(
                onExportLog = onExportLog,
                onShareLog = onShareLog,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 10.dp, horizontal = 22.dp)
        ) {
            item("INSTALL_INFO") {
                PatchOption(
                    name = stringResource(R.string.log_section_install_info),
                    description = null,
                ) {
                    SelectableTextArea(
                        text = """
                            Installation ID: ${data.id}
                            Installation Date: ${data.installDate}
                        """.trimIndent(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item("ENVIRONMENT_INFO") {
                PatchOption(
                    name = stringResource(R.string.log_section_env_info),
                    description = null,
                ) {
                    SelectableTextArea(
                        text = data.environmentInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (data.errorStacktrace != null) {
                item("ERROR_STACKTRACE") {
                    PatchOption(
                        name = stringResource(R.string.log_section_error),
                        description = null,
                    ) {
                        SelectableTextArea(
                            text = data.errorStacktrace,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item("LOG") {
                PatchOption(
                    name = stringResource(R.string.log_section_log),
                    description = null,
                ) {
                    SelectableTextArea(
                        text = data.installationLog,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
