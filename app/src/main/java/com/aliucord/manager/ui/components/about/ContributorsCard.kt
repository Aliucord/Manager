package com.aliucord.manager.ui.components.about

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.models.GithubUser
import com.aliucord.manager.utils.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun ContributorsCard() = ElevatedCard(
    modifier = Modifier.padding(bottom = 8.dp).wrapContentHeight().fillMaxWidth()
) {
    val contributors = remember { mutableStateOf<Array<GithubUser>?>(null) }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.contributors),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (contributors.value != null) LazyVerticalGrid(
            cells = GridCells.Adaptive(48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(contributors.value!!) { user -> ContributorEntry(user.name) }
        } else {
            Text(stringResource(R.string.loading))

            LaunchedEffect(null) {
                launch(Dispatchers.IO) {
                    try {
                        contributors.value = Github.contributors
                    } catch (err: Throwable) {
                        Log.e(BuildConfig.TAG, "Failed to load contributors", err)
                    }
                }
            }
        }
    }
}