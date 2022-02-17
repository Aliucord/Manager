package com.aliucord.manager.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.about.ContributorsCard
import com.aliucord.manager.ui.components.about.TeamCard

@ExperimentalFoundationApi
@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Column {
        TeamCard()
        ContributorsCard()

        Spacer(Modifier.weight(1f, true))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri("https://github.com/Aliucord/Aliucord") }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_account_github_white_24dp),
                contentDescription = "GitHub",
                modifier = Modifier.padding(8.dp)
            )
            Text(stringResource(R.string.source_code))
        }
    }
}