/**
 * MIT License
 *
 * Copyright (c) 2025 zt64
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// Adapted from compose-pipette:
// https://github.com/zt64/compose-pipette/blob/3e9fd958a315dceb142bf30250b4614ecde4e723/sample/src/commonMain/kotlin/dev/zt64/compose/pipette/sample/SampleSlider.kt

package com.aliucord.manager.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

@Composable
fun InteractiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    brush: Brush,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember(::MutableInteractionSource)

    Slider(
        value = value,
        onValueChange = onValueChange,
        thumb = {
            val interactions = remember { mutableStateListOf<Interaction>() }

            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> interactions.add(interaction)
                        is PressInteraction.Release -> interactions.remove(interaction.press)
                        is PressInteraction.Cancel -> interactions.remove(interaction.press)
                        is DragInteraction.Start -> interactions.add(interaction)
                        is DragInteraction.Stop -> interactions.remove(interaction.start)
                        is DragInteraction.Cancel -> interactions.remove(interaction.start)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .hoverable(interactionSource = interactionSource),
            ) {
                val visualSize by remember {
                    derivedStateOf {
                        if (interactions.isNotEmpty()) 28.dp else 24.dp
                    }
                }

                Spacer(
                    modifier = Modifier
                        .size(visualSize)
                        .align(Alignment.Center)
                        .background(thumbColor, CircleShape),
                )
            }
        },
        track = {
            Canvas(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .height(12.dp)
                    .fillMaxWidth(),
            ) {
                drawLine(
                    brush = brush,
                    start = Offset(0f, size.center.y),
                    end = Offset(size.width, size.center.y),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
            }
        },
        valueRange = valueRange,
        interactionSource = interactionSource,
        modifier = modifier,
    )
}
