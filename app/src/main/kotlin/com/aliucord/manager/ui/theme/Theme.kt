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
    val dynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val darkTheme = when (theme) {
        Theme.System -> if(isSystemInDarkTheme()){1} else 2
        Theme.Dark -> 1
        Theme.Light -> 2
        Theme.Black -> 3
    }
    val colorScheme = when {
        dynamicColor && darkTheme == 1 -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && darkTheme == 2-> dynamicLightColorScheme(LocalContext.current)
        dynamicColor && darkTheme == 3 -> dynamicDarkColorScheme(LocalContext.current).toPitchBlack()

        darkTheme == 1 -> darkColorScheme()
        darkTheme == 3 -> darkColorScheme().toPitchBlack()
        else -> lightColorScheme()
    }
    val customColors = when (darkTheme) {
        2 -> LightCustomColors
        else -> DarkCustomColors

    }

    // As usual, Google deprecates accompanist libraries and replaces them with an incomplete and shitty replacement in androidx
    // enableEdgeToEdge() does not work for our use case.
    @Suppress("DEPRECATION")
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colorScheme.background,
            darkIcons = darkTheme == 2,
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
    fun toDisplayName() = when (this) {
        System -> stringResource(R.string.theme_system)
        Light -> stringResource(R.string.theme_light)
        Dark -> stringResource(R.string.theme_dark)
        Black -> stringResource(R.string.theme_black)
    }

    @Composable
    fun toPainter() = when (this) {
        System -> painterResource(R.drawable.ic_sync)
        Light -> painterResource(R.drawable.ic_light)
        Dark -> painterResource(R.drawable.ic_night)
        Black -> painterResource(R.drawable.ic_night)
    }
}
fun ColorScheme.toPitchBlack(): ColorScheme {
    return this.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color.Black, // Optional: Makes cards/bars black too
        onBackground = Color.White,
        onSurface = Color.White
    )
}
