package com.aliucord.manager.ui.screens.iconopts.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import java.util.Locale
import kotlin.math.roundToInt

private fun formatRGBComponent(value: Float): String {
    return (value * 255).roundToInt()
        .toString(16)
        .padStart(2, '0')
}

private fun formatRGBColor(color: Color): String {
    return (formatRGBComponent(color.red) + formatRGBComponent(color.green) + formatRGBComponent(color.blue))
        .uppercase(Locale.ENGLISH)
}

@Composable
fun RGBTextField(
    initialColor: Color,
    setColor: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable(initialColor) { mutableStateOf(formatRGBColor(initialColor)) }

    // Clear focus from TextField if the starting color has changed
    val focusManager = LocalFocusManager.current
    DisposableEffect(initialColor) {
        focusManager.clearFocus()

        onDispose {}
    }

    TextField(
        value = text,
        onValueChange = { newText ->
            val cleaned = newText.removePrefix("#").uppercase()
            val newColor = cleaned
                .toLongOrNull(radix = 16)?.let(::Color)
                ?.copy(alpha = 1f)

            text = cleaned
            isError = newColor == null
            newColor?.let(setColor)
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        label = { Text(stringResource(R.string.iconopts_hex_title)) },
        prefix = { Text("# ") },
        modifier = modifier
            .widthIn(min = 200.dp, max = 300.dp),
    )
}
