/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.aliucord.manager.models.Commit
import com.aliucord.manager.models.GithubUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.net.URL

data class Versions(val versionCode: String, val versionName: String, val aliucordHash: String)

object Github {
    private const val org = "Aliucord"
    private const val repo = "Aliucord"

    private const val commitsUrl = "https://api.github.com/repos/$org/$repo/commits"
    private const val contributorsUrl = "https://api.github.com/repos/$org/$repo/contributors"
    private const val dataUrl = "https://raw.githubusercontent.com/$org/$repo/builds/data.json"

    fun checkForUpdates() {
        // TODO
    }

    val versions: Versions by lazy {
        gson.fromJson(InputStreamReader(URL(dataUrl).openStream()), Versions::class.java)
    }

    private val commitsCache = HashMap<String, Array<Commit>>()
    private val commitsType = object : TypeToken<Array<Commit>>() {}.type
    private val contributorsType = object : TypeToken<Array<GithubUser>>() {}.type
    private val gson = Gson()

    fun getCommits(params: Map<String, String>?): Array<Commit> {
        // return emptyArray()
        val query = params?.map { it.key + "=" + it.value }?.joinToString("&", "?") ?: ""
        if (commitsCache.containsKey(query)) return commitsCache[query] ?: emptyArray()
        val res = InputStreamReader(URL(commitsUrl + query).openStream()).use {
            gson.fromJson<Array<Commit>>(
                it,
                commitsType
            )
        }
        commitsCache[query] = res
        return res
    }

    val contributors: Array<GithubUser> by lazy {
        InputStreamReader(URL(contributorsUrl).openStream()).use { stream ->
            gson.fromJson<Array<GithubUser>>(
                stream,
                contributorsType
            ).apply {
                sortByDescending { it.contributions }
            }
        }
    }

    fun getDownloadUrl(ref: String, file: String): String {
        return "https://raw.githubusercontent.com/${org}/${repo}/${ref}/${file}";
    }
}
