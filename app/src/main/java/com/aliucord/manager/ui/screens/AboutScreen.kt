package com.aliucord.manager.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.models.GithubUser
import com.aliucord.manager.utils.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun AboutScreen() {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val modifier = Modifier.padding(8.dp)
    val contributors = remember { mutableStateOf<Array<GithubUser>?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.team),
                style = MaterialTheme.typography.h4,
            )

            UserEntry("Juby210", "Fox")
            UserEntry("torvalds", "Epic")
            UserEntry("cyyynthia", "PSA: Uninstall Client mods by Cynthia")
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.contributors),
                style = MaterialTheme.typography.h4,
            )

            if (contributors.value == null) {
                Text(
                    stringResource(R.string.loading)
                )
                LaunchedEffect(null) {
                    coroutineScope.launch {
                        try {
                            contributors.value = Github.contributors
                        } catch (err: Throwable) {
                            Log.e(BuildConfig.TAG, "Failed to load contributors", err)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    cells = GridCells.Adaptive(48.dp)
                ) {
                    items(contributors.value!!) {
                        ContributorEntry(it.login)
                    }
                }
            }
        }
    }
}

@Composable
fun UserEntry(name: String, roles: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp), shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp, 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberImagePainter(
                        data = "https://github.com/$name.png",
                        builder = {
                            transformations(CircleCropTransformation())
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.h6,
                    )
                    Text(roles)
                }
            }
        }
    }
}

@Composable
fun ContributorEntry(name: String) {
    val handler = LocalUriHandler.current
    Image(
        painter = rememberImagePainter(
            data = "https://github.com/$name.png",
        ),
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .clickable { handler.openUri("https://github.com/$name") }
    )
}
