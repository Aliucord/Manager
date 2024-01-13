package com.aliucord.manager.ui.component.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import com.aliucord.manager.R
import com.aliucord.manager.network.dto.Commit
import com.aliucord.manager.ui.component.LoadFailure
import com.aliucord.manager.ui.util.annotatingStringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

@Composable
fun CommitList(
    commits: Flow<PagingData<Commit>>,
    onRetry: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val lazyPagingItems = commits.collectAsLazyPagingItems()

            Text(
                modifier = Modifier.padding(20.dp, 20.dp, 20.dp),
                text = stringResource(R.string.commits),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                when (lazyPagingItems.loadState.refresh) {
                    LoadState.Loading -> item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    is LoadState.Error -> item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadFailure(onRetry = {
                                lazyPagingItems.retry()
                                onRetry()
                            })
                        }
                    }

                    is LoadState.NotLoading -> {
                        items(lazyPagingItems) { commit ->
                            if (commit != null) {
                                CommitItem(commit)
                            }
                        }
                    }
                }

                when (lazyPagingItems.loadState.append) {
                    LoadState.Loading -> item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    is LoadState.Error -> item {
                        LoadFailure(onRetry = {
                            lazyPagingItems.retry()
                        })
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun CommitItem(
    commit: Commit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri(commit.htmlUrl)
            }
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            commit.commit.message.split("\n").first(),
            style = MaterialTheme.typography.bodyMedium
        )

        if (commit.author != null) Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                model = "https://github.com/${commit.author.name}.png",
                contentDescription = commit.author.name
            )

            Text(
                annotatingStringResource(
                    R.string.contributors_commit_title,
                    persistentListOf(
                        commit.author.name,
                        commit.sha.take(8),
                    )
                ) {
                    when (it) {
                        "name" -> SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        "middle" -> SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        "commit" -> SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        else -> null
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
