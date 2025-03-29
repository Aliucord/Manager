package com.aliucord.manager.ui.screens.iconopts

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement

class IconOptionsModel(
    prefilledOptions: IconReplacement,
) : ScreenModel {
    var selectedColor by mutableStateOf<Color>(((prefilledOptions as? IconReplacement.CustomColor) ?: IconReplacement.Discord).color)
        private set

    fun changeSelectedColor(color: Color) {
        this.selectedColor = color
    }

    fun generateConfig(): IconReplacement {
        // TODO: other branches
        return IconReplacement.CustomColor(color = selectedColor)
    }

    enum class Mode {
        Original,
        ReplaceColor,
        ReplaceForeground,
    }
}
