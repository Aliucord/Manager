package com.aliucord.manager.ui.screens.iconopts

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement

class IconOptionsModel(
    prefilledOptions: IconReplacement,
) : ScreenModel {
    // ---------- Icon patching mode ---------- //
    var mode by mutableStateOf(IconOptionsMode.Original)
        private set

    fun changeMode(mode: IconOptionsMode) {
        this.mode = mode
    }

    // ---------- Replacement color ---------- //
    var selectedColor by mutableStateOf<HSVColorState>(IconReplacement.Aliucord.color.toHSVState())
        private set

    fun initSelectedColor(color: Color) {
        selectedColor = color.toHSVState()
    }

    // ---------- Other ---------- //
    init {
        when (prefilledOptions) {
            is IconReplacement.CustomColor if prefilledOptions.color == IconReplacement.Aliucord.color -> {
                changeMode(IconOptionsMode.Aliucord)
                initSelectedColor(IconReplacement.Aliucord.color)
            }

            is IconReplacement.CustomColor -> {
                changeMode(IconOptionsMode.CustomColor)
                initSelectedColor(prefilledOptions.color)
            }

            is IconReplacement.CustomImage -> {
                changeMode(IconOptionsMode.CustomImage)
                TODO()
            }

            IconReplacement.Original -> changeMode(IconOptionsMode.Original)
        }
    }

    fun generateConfig(): IconReplacement = when (mode) {
        IconOptionsMode.Original -> IconReplacement.Original
        IconOptionsMode.Aliucord -> IconReplacement.Aliucord
        IconOptionsMode.CustomColor -> IconReplacement.CustomColor(color = selectedColor.toARGB())
        IconOptionsMode.CustomImage -> TODO()
    }
}
