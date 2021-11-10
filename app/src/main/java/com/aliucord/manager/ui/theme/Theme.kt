/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aliucord.manager.preferences.themePreference

private val LightColorPalette = lightColors(
    primary = primaryColor,
    primaryVariant = primaryColorDark,
    secondary = primaryColorLight,
    error = errorColor,
)

private val DarkColorPalette = darkColors(
    primary = primaryColor,
    primaryVariant = primaryColorDark,
    secondary = primaryColorLight,
    error = errorColor,

    onPrimary = Color.White,
    background = darkBackground,
    surface = Color(0xff424242),
)

private val BlackColorPalette = darkColors(
    primary = primaryColor,
    primaryVariant = primaryColorDark,
    secondary = primaryColorLight,
    error = errorColor,

    onPrimary = Color.White,
    background = Color(0xff000000),
    surface = Color(0xff121212)
)

@Composable
fun isDark() = when (themePreference.value.value) {
    0 -> isSystemInDarkTheme()
    1 -> false
    else -> true
}

@Composable
fun getTheme() = if (!isDark()) LightColorPalette else when (themePreference.value.value) {
    3 -> BlackColorPalette
    else -> DarkColorPalette
}

@Composable
fun AliucordManagerTheme(content: @Composable () -> Unit) {
    val colors = getTheme()

    MaterialTheme(
        colors,
        typography = Typography,
        shapes = Shapes,
        content
    )
}
