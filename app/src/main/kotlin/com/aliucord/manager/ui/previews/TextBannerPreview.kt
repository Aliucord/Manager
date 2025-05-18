package com.aliucord.manager.ui.previews

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.components.customColors
import com.aliucord.manager.ui.screens.patching.components.TextBanner

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ButtonVotePreview(
    @PreviewParameter(TextBannerParametersProvider::class)
    parameters: TextBannerParameters,
) {
    ManagerTheme {
        TextBanner(
            text = parameters.text(),
            icon = parameters.icon(),
            iconColor = parameters.iconColor(),
            outlineColor = parameters.outlineColor(),
            containerColor = parameters.containerColor(),
        )
    }
}

private data class TextBannerParameters(
    val text: @Composable () -> String,
    val icon: @Composable () -> Painter,
    val iconColor: @Composable () -> Color,
    val outlineColor: @Composable () -> Color?,
    val containerColor: @Composable () -> Color,
)

private class TextBannerParametersProvider : PreviewParameterProvider<TextBannerParameters> {
    override val values = sequenceOf(
        TextBannerParameters(
            text = { stringResource(R.string.installer_banner_minimization) },
            icon = { painterResource(R.drawable.ic_warning) },
            iconColor = { MaterialTheme.customColors.onWarningContainer },
            outlineColor = { MaterialTheme.customColors.warning },
            containerColor = { MaterialTheme.customColors.warningContainer },
        ),
        TextBannerParameters(
            text = { stringResource(R.string.installer_banner_failure) },
            icon = { painterResource(R.drawable.ic_warning) },
            iconColor = { MaterialTheme.colorScheme.error },
            outlineColor = { null },
            containerColor = { MaterialTheme.colorScheme.errorContainer },
        ),
        TextBannerParameters(
            text = { stringResource(R.string.installer_banner_success) },
            icon = { painterResource(R.drawable.ic_check_circle) },
            iconColor = { Color(0xFF59B463) },
            outlineColor = { MaterialTheme.colorScheme.surfaceVariant },
            containerColor = { MaterialTheme.colorScheme.surfaceContainerHigh },
        )
    )
}
