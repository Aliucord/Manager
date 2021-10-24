/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@ExperimentalMaterialApi
@Composable
fun ExposedDropdownMenu(
    items: Array<String>,
    selectedItem: Int,
    selectItem: (item: Int) -> Unit = {},
    width: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val rotateAnimation by animateFloatAsState(
        targetValue = if (expanded) 180F else 0F,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
    )

    Column(modifier = Modifier.width(width)) {
        ListItem(
            text = { Text(items[selectedItem]) },
            trailing = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotateAnimation)
                )
            },
            modifier = Modifier.clickable { expanded = !expanded }
        )
        if (expanded) Divider(color = MaterialTheme.colors.primary) else Divider()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(width)
        ) {
            items.forEachIndexed { i, item ->
                DropdownMenuItem(
                    onClick = { selectItem(i) }
                ) {
                    Text(item)
                }
            }
        }
    }
}
