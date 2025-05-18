package com.aliucord.manager.ui.screens.log.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliucord.manager.ui.util.horizontalScrollbar
import com.aliucord.manager.ui.util.thenIf

@Composable
fun LogTextArea(
    text: String,
    modifier: Modifier = Modifier.Companion,
) {
    val scrollState = rememberScrollState()
    val scrollable by remember { derivedStateOf { scrollState.maxValue > 0 } }

    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Companion.Monospace,
            softWrap = false,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
                .horizontalScroll(scrollState)
                .horizontalScrollbar(scrollState)
                .thenIf(scrollable) { padding(bottom = 10.dp) }
        )
    }
}
