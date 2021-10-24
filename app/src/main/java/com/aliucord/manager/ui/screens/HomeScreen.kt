/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aliucord.manager.ui.components.CommitsList

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val selectedCommit = remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card {
            ListItem(
                text = { Text("Aliucord") },
                secondaryText = {
                    Text(buildAnnotatedString {
                        append("Supported version: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(selectedCommit.value ?: "blah")
                        }
                    })
                },
                trailing = {
                    //TODO button isn't centered for some reasons
                    TextButton(
                        onClick = { navController.navigate("install") }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = "install",
                        )
                        Text("Install")
                    }
                },
                singleLineSecondaryText = true,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Card { CommitsList(selectedCommit) }
    }
}
