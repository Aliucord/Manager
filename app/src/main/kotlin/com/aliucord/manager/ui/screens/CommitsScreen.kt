package com.aliucord.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.aliucord.manager.R
import com.aliucord.manager.models.github.Commit
import com.aliucord.manager.ui.components.ListItem
import com.aliucord.manager.utils.Github
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun CommitsScreen() {
    val pager = remember {
        Pager(
            PagingConfig(
                pageSize = 30,
                enablePlaceholders = true,
                maxSize = 200
            )
        ) {
            object : PagingSource<Int, Commit>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Commit> {
                    val pageNumber = params.key ?: 0

                    val response = Github.getCommits("page" to pageNumber.toString())
                    val prevKey = if (pageNumber > 0) pageNumber - 1 else null
                    val nextKey = if (response.isNotEmpty()) pageNumber + 1 else null

                    return LoadResult.Page(
                        data = response,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }

                override fun getRefreshKey(state: PagingState<Int, Commit>) = state.anchorPosition?.let {
                    state.closestPageToPosition(it)?.prevKey?.plus(1) ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
                }
            }
        }
    }

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) item {
            Text(
                text = stringResource(R.string.paging_initial_load),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }

        items(lazyPagingItems) { commitData ->
            val localUriHandler = LocalUriHandler.current

            if (commitData == null) return@items

            ListItem(
                modifier = Modifier.clickable {
                    localUriHandler.openUri(commitData.htmlUrl)
                },
                overlineText = { Text(commitData.sha.substring(0, 7)) },
                text = { Text("${commitData.commit.message.split("\n").first()} - ${commitData.author.name}")}
            )
        }

        if (lazyPagingItems.loadState.append == LoadState.Loading) item {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}