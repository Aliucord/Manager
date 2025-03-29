package com.aliucord.manager.ui.screens.iconopts

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.aliucord.manager.ui.screens.iconopts.components.IconOptionsAppBar
import dev.zt64.compose.pipette.CircularColorPicker
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

interface IconOptionsScreenParent : Parcelable {
    /**
     * The screen model is elevated outside of this screen
     * in order to share the state with the parent screen.
     */
    @Composable
    fun getIconModel(): IconOptionsModel
}

@Parcelize
class IconOptionsScreen(
    private val parent: IconOptionsScreenParent,
) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "IconOptions"

    @Composable
    override fun Content() {
        val model = parent.getIconModel()

        IconOptionsScreenContent(
            selectedColor = model.selectedColor,
            setSelectedColor = model::changeSelectedColor,
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
