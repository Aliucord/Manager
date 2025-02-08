package com.aliucord.manager.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.SegmentedButton
import com.aliucord.manager.ui.components.VersionDisplay
import com.aliucord.manager.ui.screens.home.InstallData

@Composable
fun InstalledItemCard(
    data: InstallData,
    onUpdate: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenPlugins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
        ),
        modifier = modifier
            .width(IntrinsicSize.Max),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = data.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                )

                Column {
                    Text(
                        text = data.name,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .94f),
                    )

                    Text(
                        text = data.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 1.dp)
                            .offset(y = (-2).dp)
                            .alpha(.7f)
                            .basicMarquee(),
                    )
                }

                Spacer(Modifier.weight(1f, fill = true))

                VersionDisplay(
                    version = data.version,
                    prefix = { append("v") },
                    modifier = Modifier
                        .alpha(.6f)
                        .padding(end = 4.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clip(MaterialTheme.shapes.large),
            ) {
                SegmentedButton(
                    icon = painterResource(R.drawable.ic_extension),
                    text = stringResource(R.string.plugins_title),
                    onClick = onOpenPlugins,
                )
                SegmentedButton(
                    icon = painterResource(R.drawable.ic_info),
                    text = stringResource(R.string.action_open_info),
                    onClick = onOpenInfo,
                )

                if (data.isUpToDate) {
                    SegmentedButton(
                        icon = painterResource(R.drawable.ic_launch),
                        text = stringResource(R.string.action_launch),
                        onClick = onOpenApp,
                    )
                } else {
                    val warningColor = Color(0xFFFFBB33)

                    SegmentedButton(
                        icon = painterResource(R.drawable.ic_update),
                        text = stringResource(R.string.action_update),
                        iconColor = warningColor,
                        textColor = warningColor,
                        onClick = onUpdate,
                    )
                }
            }
        }
    }
}
