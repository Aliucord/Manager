/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.about.ContributorsCard
import com.aliucord.manager.ui.components.about.TeamCard
import com.aliucord.manager.ui.viewmodels.AboutViewModel
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    val viewModel: AboutViewModel = viewModel()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeamCard()
        ContributorsCard(viewModel.contributors)

        Spacer(Modifier.weight(1f, true))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri("https://github.com/Aliucord/Aliucord") }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_account_github_white_24dp),
                contentDescription = "GitHub"
            )
            Spacer(Modifier.padding(4.dp))
            Text(stringResource(R.string.source_code))
        }
    }
}
