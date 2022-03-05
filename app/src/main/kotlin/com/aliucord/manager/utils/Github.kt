/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.aliucord.manager.models.github.Commit
import com.aliucord.manager.models.github.Version
import io.ktor.client.request.*
import java.io.InputStreamReader
import java.net.URL
import kotlin.collections.set

object Github {
    private const val org = "Aliucord"
    private const val repo = "Aliucord"

    private const val commitsUrl = "https://api.github.com/repos/$org/$repo/commits"
    const val contributorsUrl = "https://api.github.com/repos/$org/$repo/contributors"
    const val dataUrl = "https://raw.githubusercontent.com/$org/$repo/builds/data.json"

    fun checkForUpdates() {
        // TODO
    }

    val version: Version by lazy {
        gson.fromJson(InputStreamReader(URL(dataUrl).openStream()), Version::class.java)
    }

    private val commitsCache = HashMap<String, List<Commit>>()

    suspend fun getCommits(vararg params: Pair<String, String>): List<Commit> {
        val query = params.joinToString("&", "?") { it.first + "=" + it.second }

        if (commitsCache.containsKey(query)) return commitsCache[query] ?: emptyList()

        val res = httpClient.get<List<Commit>>(commitsUrl) {
            params.forEach { (key, value) -> parameter(key, value) }
        }

        commitsCache[query] = res

        return res
    }
}