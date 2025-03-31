package com.aliucord.manager.ui.screens.iconopts

import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.InteractiveSlider
import com.aliucord.manager.ui.components.MainActionButton
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
            selectedImage = { model.selectedImage },
            setSelectedImage = model::changeSelectedImageUri,
        )
    }
}

@Composable
fun IconOptionsScreenContent(
    mode: IconOptionsMode,
    setMode: (IconOptionsMode) -> Unit,
    selectedColor: HSVColorState,
    selectedImage: () -> ByteArray?,
    setSelectedImage: (Uri) -> Unit,
) {
    Scaffold(
        topBar = { IconOptionsAppBar() },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
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
            RadioSelectorItem(
                name = stringResource(R.string.iconopts_variant_title_image),
                description = stringResource(R.string.iconopts_variant_desc_image),
                selected = mode == IconOptionsMode.CustomImage,
                onClick = remember { { setMode(IconOptionsMode.CustomImage) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            AnimatedVisibility(
                visible = mode == IconOptionsMode.CustomColor,
                enter = fadeIn(tween(delayMillis = 250)),
                exit = fadeOut(tween(durationMillis = 200)),
            ) {
                CustomColorOptions(selectedColor)
            }

            AnimatedVisibility(
                visible = mode == IconOptionsMode.CustomImage,
                enter = fadeIn(tween(delayMillis = 250)),
                exit = fadeOut(tween(durationMillis = 200)),
            ) {
                CustomImageOptions(
                    selectedImage = selectedImage,
                    setSelectedImage = setSelectedImage,
                )
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

@Composable
private fun CustomImageOptions(
    selectedImage: () -> ByteArray?,
    setSelectedImage: (Uri) -> Unit,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { it?.let(setSelectedImage) }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = Modifier.padding(horizontal = 15.dp)
    ) {
        MainActionButton(
            text = stringResource(R.string.iconopts_btn_open_selection),
            icon = painterResource(R.drawable.ic_launch),
            onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )

        val uriHandler = LocalUriHandler.current
        MainActionButton(
            text = stringResource(R.string.iconopts_btn_open_example_image),
            icon = painterResource(R.drawable.ic_launch),
            onClick = { uriHandler.openUri("https://github.com/Aliucord/Aliucord/blob/fe8b17002ec1ee66ac3eeb2855c9ca2f2f307410/installer/android/app/src/main/assets/icon1.png") },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )

        PatchOption(
            name = stringResource(R.string.iconopts_image_preview_title),
            description = stringResource(R.string.iconopts_image_preview_desc),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Crossfade(
                targetState = selectedImage(),
                label = "Selected image preview fade",
                animationSpec = tween(durationMillis = 300, delayMillis = 250),
            ) { image ->
                if (image == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Text(
                            text = stringResource(R.string.iconopts_image_preview_none),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.alpha(.5f),
                        )
                    }
                } else {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(MaterialTheme.shapes.large),
                    )
                }
            }
        }
    }
}
