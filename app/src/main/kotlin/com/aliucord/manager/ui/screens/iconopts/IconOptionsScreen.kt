package com.aliucord.manager.ui.screens.iconopts

import android.os.Parcelable
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.InteractiveSlider
import com.aliucord.manager.ui.screens.iconopts.components.*
import com.aliucord.manager.ui.screens.patchopts.components.options.PatchOption
import com.aliucord.manager.ui.util.ColorSaver
import dev.zt64.compose.pipette.CircularColorPicker
import dev.zt64.compose.pipette.util.*
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
            mode = model.mode,
            setMode = model::changeMode,
            selectedColor = model.selectedColor,
        )
    }
}

@Composable
fun IconOptionsScreenContent(
    mode: IconOptionsMode,
    setMode: (IconOptionsMode) -> Unit,
    selectedColor: HSVColorState,
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
            // TODO: live icon preview

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            RadioSelectorItem(
                name = stringResource(R.string.discord),
                description = stringResource(R.string.iconopts_variant_desc_discord),
                selected = mode == IconOptionsMode.Original,
                onClick = remember { { setMode(IconOptionsMode.Original) } },
            )
            RadioSelectorItem(
                name = stringResource(R.string.aliucord),
                description = stringResource(R.string.iconopts_variant_desc_aliucord),
                selected = mode == IconOptionsMode.Aliucord,
                onClick = remember { { setMode(IconOptionsMode.Aliucord) } },
            )
            RadioSelectorItem(
                name = stringResource(R.string.iconopts_variant_title_color),
                description = stringResource(R.string.iconopts_variant_desc_color),
                selected = mode == IconOptionsMode.CustomColor,
                onClick = remember { { setMode(IconOptionsMode.CustomColor) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            AnimatedVisibility(
                visible = mode == IconOptionsMode.CustomColor,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically { -it / 10 },
            ) {
                CustomColorOptions(selectedColor)
            }
        }
    }
}

@Composable
private fun CustomColorOptions(color: HSVColorState) {
    // This color is separated from the live color and intentionally lags behind while the RGBTextField is being edited.
    // When this changes, then the text inside the RGBTextField is reset to the fully formatted color.
    // As such, this only happens when the color is changed via the other color pickers.
    var initialRGBFieldColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(color.toARGB()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier.padding(horizontal = 15.dp)
    ) {
        PatchOption(
            name = stringResource(R.string.iconopts_colorpicker_title),
            description = stringResource(R.string.iconopts_colorpicker_desc),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularColorPicker(
                hue = color.hue,
                saturation = color.saturation,
                value = color.value,
                onColorChange = { hue, saturation ->
                    color.hue = hue
                    color.saturation = saturation
                    initialRGBFieldColor = color.toARGB()
                },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(260.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        PatchOption(
            name = stringResource(R.string.iconopts_lightness_title),
            description = stringResource(R.string.iconopts_lightness_desc),
        ) {
            InteractiveSlider(
                value = color.value,
                onValueChange = {
                    color.value = it
                    initialRGBFieldColor = color.toARGB()
                },
                valueRange = 0f..1f,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        Color.hsv(color.hue, color.saturation, 1f)
                    ),
                ),
                thumbColor = color.toARGB(),
            )
        }

        PatchOption(
            name = stringResource(R.string.iconopts_hex_title),
            description = stringResource(R.string.iconopts_hex_desc),
        ) {
            RGBTextField(
                initialColor = initialRGBFieldColor,
                setColor = {
                    color.hue = it.hue
                    color.saturation = it.saturation
                    color.value = it.hsvValue
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
