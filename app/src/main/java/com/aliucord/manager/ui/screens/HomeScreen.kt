/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aliucord.manager.ui.components.CommitsList

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun HomeScreen() {
    val selectedCommit = remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.padding(8.dp)) {
        Card(modifier = Modifier.padding(bottom = 8.dp)) {
            ListItem(
                text = { Text("Aliucord") },
                secondaryText = { Text(buildAnnotatedString {
                    append("Supported version: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(selectedCommit.value ?: "blah") }
                }) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Card { CommitsList(selectedCommit) }
    }
}
