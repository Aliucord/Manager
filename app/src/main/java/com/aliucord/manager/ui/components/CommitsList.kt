/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.models.Commit
import com.aliucord.manager.ui.theme.isDark
import com.aliucord.manager.utils.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private class CommitData(
    val commit: Commit,
    val buildSha: String?
)

@Composable
fun CommitsList(selectedCommit: MutableState<String?>, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var isLoading by remember { mutableStateOf(true) }
    val commits = remember { mutableStateListOf<CommitData>() }

    if (isLoading) {
        ConstraintLayout(modifier = modifier
            .fillMaxSize()
            .padding(12.dp)) {
            CommitsHeader()
            CircularProgressIndicator(modifier = Modifier.constrainAs(createRef()) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })
        }

        LaunchedEffect(null) {
            coroutineScope.launch {
                try {
                    val buildCommits = Github.getCommits(mapOf("sha" to "builds", "path" to "Aliucord.dex", "per_page" to "50"))
                    val commits2 = Github.getCommits(mapOf("per_page" to "50"))
                    commits.addAll(commits2.map { c -> CommitData(c, buildCommits.find { bc -> bc.commit.message.substring(6) == c.sha }?.sha) })
                    isLoading = false
                    if (commits.size > 0) selectedCommit.value = commits.firstOrNull { it.buildSha != null }?.buildSha
                } catch (e: Throwable) {
                    Log.e(BuildConfig.TAG, "Failed to get commits", e)
                }
            }
        }
    } else {
        val dark = isDark()

        // LazyColumn causes too much lags, also you can't get scrollbar on it, so I used AndroidView with RecyclerView for now
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            CommitsHeader()
            AndroidView({ context -> View.inflate(context, R.layout.recycler, null).apply {
                val rv = this as RecyclerView
                rv.adapter = CommitsAdapter(commits, dark)
                rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }})
        }
    }
}

@Composable
private fun CommitsHeader() {
    Text(
        stringResource(R.string.commits),
        style = MaterialTheme.typography.subtitle1,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

private class CommitsAdapter(private val commits: List<CommitData>, private val dark: Boolean) : RecyclerView.Adapter<CommitsAdapter.ViewHolder>() {
    class ViewHolder(view: View, dark: Boolean) : RecyclerView.ViewHolder(view) {
        val sha: TextView = view.findViewById(R.id.commit_sha)
        val message: TextView = view.findViewById(R.id.commit_message)

        init {
            message.setTextColor(if (dark) 0xccffffff.toInt() else 0xcc000000.toInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.commit, parent, false), dark)

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = commits[position]
        val commit = data.commit
        val color = if (data.buildSha != null) holder.sha.context.getColor(R.color.primary) else Color.GRAY

        holder.sha.text = commit.sha.substring(0, 7)
        holder.sha.setTextColor(color)
        holder.sha.setOnClickListener { it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(commit.htmlUrl))) }

        holder.message.text = "${commit.commit.message.split("\n")[0]} - ${commit.author.name}"
    }

    override fun getItemCount() = commits.size
}
