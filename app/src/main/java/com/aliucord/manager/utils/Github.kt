/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.aliucord.manager.models.Commit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.net.URL

object Github {
    private const val org = "Aliucord"
    private const val repo = "Aliucord"

    private const val commitsUrl = "https://api.github.com/repos/${org}/${repo}/commits"

    fun checkForUpdates() {
        // TODO
    }

    private val commitsCache = HashMap<String, Array<Commit>>()
    private val commitsType = object : TypeToken<Array<Commit>>() {}.type
    private val gson = Gson()

    fun getCommits(params: Map<String, String>?): Array<Commit> {
        // return emptyArray()
        val query = params?.map { it.key + "=" + it.value }?.joinToString("&", "?") ?: ""
        if (commitsCache.containsKey(query)) return commitsCache[query] ?: emptyArray()
        val res = gson.fromJson<Array<Commit>>(
            InputStreamReader(URL(commitsUrl + query).openStream()),
            commitsType
        )
        commitsCache[query] = res
        return res
    }

    fun getDownloadUrl(ref: String, file: String): String {
        return "'https://raw.githubusercontent.com/${org}/${repo}/${ref}/${file}"
    }
}
