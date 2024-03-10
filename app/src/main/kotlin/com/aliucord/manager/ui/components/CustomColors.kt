package com.aliucord.manager.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

val MaterialTheme.customColors: CustomColors
    @Composable
    inline get() = LocalCustomColors.current

val LocalCustomColors = staticCompositionLocalOf<CustomColors> {
    error("No LocalCustomColors provided!")
}

@Immutable
data class CustomColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val YellowAlt1 = Color(0xFFE9C414)
private val Shandy = Color(0xFFFFE172)
private val DarkBrown = Color(0xFF3B2F00)
private val DarkerBrown = Color(0xFF221B00)
private val DarkBronze = Color(0xFF554600)

val DarkCustomColors = CustomColors(
    warning = YellowAlt1,
    onWarning = DarkBrown,
    warningContainer = DarkBronze,
    onWarningContainer = Shandy,
)

val LightCustomColors = CustomColors(
    warning = YellowAlt1,
    onWarning = Color.White,
    warningContainer = Shandy,
    onWarningContainer = DarkerBrown,
)
