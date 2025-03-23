package com.aliucord.manager.ui.screens.about.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun LeadContributor(
    name: String,
    roles: String,
    username: String = name,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .clickable(
                onClick = { uriHandler.openUri("https://github.com/$username") },
                indication = ripple(bounded = false, radius = 90.dp),
                interactionSource = remember(::MutableInteractionSource)
            )
            .widthIn(min = 100.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(71.dp)
                .clip(CircleShape),
            model = "https://github.com/$username.png",
            contentDescription = username
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                )
            )

            Text(
                text = roles,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}
