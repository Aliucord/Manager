package com.aliucord.manager.ui.screens.componentopts.components

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.network.utils.SemVer
import kotlin.time.Instant

@Composable
fun PatchComponentCard(
    version: SemVer,
    timestamp: Instant,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PatchComponentCardBase(
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_page),
            contentDescription = null,
            modifier = Modifier
                .alpha(.8f)
                .padding(end = 4.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = DateUtils.getRelativeDateTimeString(
                    /* c = */ LocalContext.current,
                    /* time = */ timestamp.toEpochMilliseconds(),
                    /* minResolution = */ DateUtils.SECOND_IN_MILLIS,
                    /* transitionResolution = */ DateUtils.WEEK_IN_MILLIS,
                    /* flags = */ DateUtils.FORMAT_ABBREV_ALL,
                ).toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(.6f),
            )
        }

        Spacer(Modifier.weight(1f, fill = true))

        IconButton(
            onClick = onDelete,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_forever),
                tint = MaterialTheme.colorScheme.error,
                contentDescription = stringResource(R.string.action_delete),
            )
        }
    }
}

@Composable
fun PatchComponentCardBase(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember(::MutableInteractionSource)

    Surface(
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interaction,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                interactionSource = interaction,
            )

            content()
        }
    }
}
