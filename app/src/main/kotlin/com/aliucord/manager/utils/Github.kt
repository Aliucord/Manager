/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.aliucord.manager.models.Commit
import com.aliucord.manager.models.Version
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

    private val commitsCache = HashMap<String, Array<Commit>>()

    suspend fun getCommits(vararg params: Pair<String, String>): Array<Commit> {
        val query = params.joinToString("&", "?") { it.first + "=" + it.second }

        if (commitsCache.containsKey(query)) return commitsCache[query] ?: emptyArray()

        val res = httpClient.get<Array<Commit>>(commitsUrl) {
            params.forEach { (key, value) -> parameter(key, value) }
        }

        commitsCache[query] = res

        return res
    }

    fun getDownloadUrl(ref: String, file: String) = "https://raw.githubusercontent.com/$org/$repo/$ref/$file"
}