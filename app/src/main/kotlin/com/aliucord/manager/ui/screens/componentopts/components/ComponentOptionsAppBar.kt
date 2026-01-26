package com.aliucord.manager.ui.screens.componentopts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.BackButton
import com.aliucord.manager.ui.screens.componentopts.PatchComponent

@Composable
fun ComponentOptionsAppBar(
    componentType: PatchComponent.Type,
) {
    TopAppBar(
        navigationIcon = { BackButton() },
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(stringResource(R.string.componentopts_screen_title, componentType.name))
                Text(
                    text = stringResource(R.string.componentopts_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(.7f),
                )
            }
        },
    )
}
