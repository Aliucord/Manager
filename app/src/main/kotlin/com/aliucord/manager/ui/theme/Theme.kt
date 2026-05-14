/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
*/

package com.aliucord.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aliucord.manager.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun ManagerTheme(
    theme: Theme = Theme.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val isDark = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        else -> true // Dark and Black
    }
    val isBlack = theme == Theme.Black

    val baseScheme = when {
        dynamicColor && isDark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        isDark -> darkColorScheme()
        else -> lightColorScheme()
    }


    val colorScheme = if (isBlack) baseScheme.toPitchBlack() else baseScheme


    val customColors = when(isDark) {
        true -> DarkCustomColors
        false -> LightCustomColors
    }

    // As usual, Google deprecates accompanist libraries and replaces them with an incomplete and shitty replacement in androidx
    // enableEdgeToEdge() does not work for our use case.
    @Suppress("DEPRECATION")
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colorScheme.background,
            darkIcons = !isDark,
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
        )
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ThemeTypography,
            content = content,
        )
    }
}

enum class Theme {
    System,
    Light,
    Dark,
    Black;

    @Composable
    fun toDisplayName() = stringResource(
        when (this) {
            System -> R.string.theme_system
            Light -> R.string.theme_light
            Dark, Black -> R.string.theme_dark
        }
    )

    @Composable
    fun toPainter() = painterResource(
        when (this) {
            System -> R.drawable.ic_sync
            Light -> R.drawable.ic_light
            Dark, Black -> R.drawable.ic_night
        }
    )
}

fun ColorScheme.toPitchBlack(): ColorScheme {
    return this.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    )
}
