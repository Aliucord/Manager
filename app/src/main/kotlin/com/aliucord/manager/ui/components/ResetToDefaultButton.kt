package com.aliucord.manager.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.util.mirrorVertically

@Composable
fun ResetToDefaultButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = enabled,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally(),
        modifier = modifier,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = stringResource(R.string.action_reset_default),
                modifier = Modifier
                    .mirrorVertically()
                    .size(26.dp),
            )
        }
    }
}
