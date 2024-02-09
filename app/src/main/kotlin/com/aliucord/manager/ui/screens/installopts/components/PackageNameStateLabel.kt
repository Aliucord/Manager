package com.aliucord.manager.ui.screens.installopts.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.installopts.PackageNameState

@Composable
fun PackageNameStateLabel(
    state: PackageNameState,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state,
        label = "PackageNameStateLabel CrossFade"
    ) { animatedState ->
        val (label, icon, tint) = when (animatedState) {
            PackageNameState.Invalid -> Triple(
                R.string.installopts_pkgname_invalid,
                R.drawable.ic_canceled,
                MaterialTheme.colorScheme.error,
            )

            PackageNameState.Taken -> Triple(
                R.string.installopts_pkgname_taken,
                R.drawable.ic_warning,
                Color(0xFFFFCC00),
            )

            PackageNameState.Ok -> Triple(
                R.string.installopts_pkgname_ok,
                R.drawable.ic_check_circle,
                Color(0xFF59B463),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )

            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(.7f),
            )
        }
    }
}
