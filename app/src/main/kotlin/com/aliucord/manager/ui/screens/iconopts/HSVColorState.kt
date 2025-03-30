package com.aliucord.manager.ui.screens.iconopts

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import dev.zt64.compose.pipette.util.*

@Stable
class HSVColorState(
    hue: Float,
    saturation: Float,
    value: Float,
) {
    var hue by mutableFloatStateOf(hue)
    var saturation by mutableFloatStateOf(saturation)
    var value by mutableFloatStateOf(value)

    fun toARGB(): Color = Color.hsv(hue, saturation, value)
    override fun toString(): String = "HSVColorState($hue, $saturation, $value)"
}

fun Color.toHSVState(): HSVColorState {
    return HSVColorState(
        hue = this.hue,
        saturation = this.saturation,
        value = this.hsvValue,
    )
}
