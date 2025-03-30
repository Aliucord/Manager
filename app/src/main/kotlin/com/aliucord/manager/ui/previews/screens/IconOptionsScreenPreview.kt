package com.aliucord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.iconopts.*
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement

// This preview has scrollable/interactable content that cannot be properly tested from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun IconOptionsScreenPreview(
    @PreviewParameter(IconOptionsParametersProvider::class)
    parameters: IconOptionsParameters,
) {
    var mode by remember { mutableStateOf(parameters.mode) }
    var color by remember { mutableStateOf(parameters.selectedColor.toHSVState()) }

    DisposableEffect(parameters) {
        mode = parameters.mode
        color = parameters.selectedColor.toHSVState()

        onDispose {}
    }

    ManagerTheme {
        IconOptionsScreenContent(
            mode = mode,
            setMode = { mode = it },
            selectedColor = color,
        )
    }
}

private data class IconOptionsParameters(
    val mode: IconOptionsMode,
    val selectedColor: Color,
)

private class IconOptionsParametersProvider : PreviewParameterProvider<IconOptionsParameters> {
    override val values = sequenceOf(
        IconOptionsParameters(
            mode = IconOptionsMode.Original,
            selectedColor = IconReplacement.Aliucord.color,
        ),
        IconOptionsParameters(
            mode = IconOptionsMode.Aliucord,
            selectedColor = IconReplacement.Aliucord.color,
        ),
        IconOptionsParameters(
            mode = IconOptionsMode.CustomColor,
            selectedColor = Color(0xFFFFB6C1),
        ),
    )
}
