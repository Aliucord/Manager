package com.aliucord.manager.ui.components.installer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aliucord.manager.R
import com.aliucord.manager.ui.Screen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallerDialog(
    visible: MutableState<Boolean>,
    navController: NavController
) = AlertDialog(
    title = {
        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_discord), "Discord")
            Spacer(Modifier.width(12.dp))
            Text("Select Installation Method")
        }
    },
    confirmButton = {
        Column(
            modifier = Modifier.wrapContentWidth().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifier = Modifier.fillMaxWidth()

            Button(
                modifier = modifier,
                onClick = {
                    navController.currentBackStackEntry?.arguments
                    navController.navigate(Screen.Installer.route)
                }
            ) {
                Text("Download")
            }

            Button(
                modifier = modifier,
                onClick = {
                    navController.navigate(Screen.Installer.route)
                }
            ) {
                Text("From installed app")
            }

            Button(
                modifier = modifier,
                onClick = {
                    navController.navigate(Screen.Installer.route)
                }
            ) {
                Text("From storage")
            }
        }
    },
    onDismissRequest = { visible.value = false }
)