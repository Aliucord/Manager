package com.aliucord.manager.ui.components.about

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard() = ElevatedCard(
    modifier = Modifier.wrapContentHeight().fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.team),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserEntry("Juby210", "Fox")
            UserEntry("Vendicated", "the ven")
            UserEntry("mwittrien", "your memory gon")
        }
    }
}