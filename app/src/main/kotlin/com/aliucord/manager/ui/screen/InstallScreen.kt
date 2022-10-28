/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screen

import android.os.Parcelable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.component.installer.*
import com.aliucord.manager.ui.dialog.DiscordType
import com.aliucord.manager.ui.dialog.DownloadMethod
import com.aliucord.manager.util.copyText
import com.aliucord.manager.util.saveFile
import com.aliucord.manager.ui.viewmodel.InstallViewModel
import kotlinx.parcelize.Parcelize
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date

@Parcelize
data class InstallData(
    val downloadMethod: DownloadMethod,
    val discordType: DiscordType,
    var baseApk: String? = null,
    var splits: List<String>? = null
) : Parcelable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    installData: InstallData,
    onBackClick: () -> Unit,
    viewModel: InstallViewModel = getViewModel(parameters = { parametersOf(installData) })
) {
    val navigateMain by viewModel.returnToHome.collectAsState(initial = false)
    val context = LocalContext.current

    if (navigateMain) onBackClick()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = stringResource(R.string.navigation_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            if(
                viewModel.steps[viewModel.currentStep]?.status == Status.ONGOING) LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 4.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                InstallStep(
                    name = "Download APKs",
                    isCurrent = viewModel.currentCategory == InstallViewModel.StepCategory.APK_DL,
                    subSteps = listOf(
                        viewModel.steps[InstallViewModel.Steps.DL_BASE_APK]!!,
                        viewModel.steps[InstallViewModel.Steps.DL_LIBS_APK]!!,
                        viewModel.steps[InstallViewModel.Steps.DL_LANG_APK]!!,
                        viewModel.steps[InstallViewModel.Steps.DL_RESC_APK]!!
                    )
                )
                InstallStep(
                    name = "Download Libraries",
                    isCurrent = viewModel.currentCategory == InstallViewModel.StepCategory.LIB_DL,
                    subSteps = listOf(
                        viewModel.steps[InstallViewModel.Steps.DL_HERMES]!!,
                        viewModel.steps[InstallViewModel.Steps.DL_ALIUNATIVE]!!
                    )
                )
                InstallStep(
                    name = "Patch APKs",
                    isCurrent = viewModel.currentCategory == InstallViewModel.StepCategory.PATCHING,
                    subSteps = if(viewModel.preferences.replaceIcon) listOf(
                        viewModel.steps[InstallViewModel.Steps.PATCH_APP_ICON]!!,
                        viewModel.steps[InstallViewModel.Steps.PATCH_MANIFEST]!!,
                        viewModel.steps[InstallViewModel.Steps.PATCH_DEX]!!,
                        viewModel.steps[InstallViewModel.Steps.PATCH_LIBS]!!
                    ) else listOf(
                        viewModel.steps[InstallViewModel.Steps.PATCH_MANIFEST]!!,
                        viewModel.steps[InstallViewModel.Steps.PATCH_DEX]!!,
                        viewModel.steps[InstallViewModel.Steps.PATCH_LIBS]!!
                    )
                )
                InstallStep(
                    name = "Install",
                    isCurrent = viewModel.currentCategory == InstallViewModel.StepCategory.INSTALLING,
                    subSteps = listOf(
                        viewModel.steps[InstallViewModel.Steps.SIGN_APK]!!,
                        viewModel.steps[InstallViewModel.Steps.INSTALL_APK]!!
                    )
                )

                if(viewModel.stacktrace.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            text = viewModel.stacktrace,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            softWrap = false,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = {
                            context.saveFile("Error ${SimpleDateFormat("hh-mm-s a MM-dd-YYYY").format(Date())}.log", "${viewModel.debugInfo}\n\n${viewModel.stacktrace}")
                        }) {
                            Text(text = stringResource(id = R.string.installer_save_file))
                        }

                        FilledTonalButton(onClick = { context.copyText(viewModel.stacktrace) }) {
                            Text(text = stringResource(id = R.string.installer_copy_text))
                        }
                    }
                }

            }
        }
    }
}
