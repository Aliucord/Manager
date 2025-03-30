package com.aliucord.manager.ui.screens.iconopts

import android.os.Parcelable
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.iconopts.components.IconOptionsAppBar
import com.aliucord.manager.ui.screens.iconopts.components.RadioSelectorItem
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
            mode = model.mode,
            setMode = model::changeMode,
            selectedColor = model.selectedColor,
            setSelectedColor = model::changeSelectedColor,
        )
    }
}

@Composable
fun IconOptionsScreenContent(
    mode: IconOptionsMode,
    setMode: (IconOptionsMode) -> Unit,
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
                CircularColorPicker(
                    color = selectedColor,
                    onColorChange = setSelectedColor,
                    modifier = Modifier.size(230.dp),
                )
            }
        }
    }
}
