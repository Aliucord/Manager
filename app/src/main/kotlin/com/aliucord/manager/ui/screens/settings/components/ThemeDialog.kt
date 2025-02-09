package com.aliucord.manager.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.Theme

@Composable
fun ThemeDialog(
    currentTheme: Theme,
    onDismissRequest: () -> Unit,
    onConfirm: (Theme) -> Unit,
) {
    var selectedTheme by rememberSaveable { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_brush),
                contentDescription = stringResource(R.string.settings_theme),
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                for (theme in Theme.entries) key(theme) {
                    val interactionSource = remember(::MutableInteractionSource)

                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        modifier = Modifier.Companion
                            .clickable(
                                indication = null,
                                interactionSource = interactionSource,
                                onClick = { selectedTheme = theme },
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = theme.toDisplayName(),
                            style = MaterialTheme.typography.labelLarge,
                        )

                        Spacer(Modifier.Companion.weight(1f, true))

                        RadioButton(
                            selected = theme == selectedTheme,
                            onClick = { selectedTheme = theme },
                            interactionSource = interactionSource,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedTheme)
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
    )
}
