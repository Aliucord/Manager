package com.aliucord.manager.ui.screens.iconopts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.aliucord.manager.ui.screens.iconopts.components.IconOptionsAppBar
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement
import dev.zt64.compose.pipette.CircularColorPicker

class IconOptionsScreen(
    private val prefilledOptions: IconReplacement,
) : Screen {
    override val key = "IconOptions"

    @Composable
    override fun Content() {
        var (color, setColor) = remember { mutableStateOf(IconReplacement.Aliucord.color) }

        IconOptionsScreenContent(
            selectedColor = color,
            setSelectedColor = setColor,
        )
    }
}

@Composable
fun IconOptionsScreenContent(
    selectedColor: Color,
    setSelectedColor: (Color) -> Unit,
) {
    Scaffold(
        topBar = { IconOptionsAppBar() },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            CircularColorPicker(
                color = selectedColor,
                onColorChange = setSelectedColor,
                modifier = Modifier.size(250.dp)
            )
        }
    }
}
