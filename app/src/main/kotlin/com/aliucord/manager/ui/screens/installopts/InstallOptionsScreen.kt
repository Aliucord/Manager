package com.aliucord.manager.ui.screens.installopts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.installopts.components.PackageNameStateLabel

class InstallOptionsScreen : Screen {
    override val key = "InstallOptions"

    @Composable
    override fun Content() {
        val model = getScreenModel<InstallOptionsModel>()

        Scaffold { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 25.dp)
            ) {
                OutlinedTextField(
                    value = model.packageName,
                    onValueChange = model::changePackageName,
                    label = { Text(stringResource(R.string.setting_package_name)) },
                    isError = model.packageNameState == PackageNameState.Invalid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PackageNameStateLabel(
                    state = model.packageNameState,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
