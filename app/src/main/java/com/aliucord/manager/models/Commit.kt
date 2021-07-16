/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.models

import com.google.gson.annotations.SerializedName

class Commit(
    @SerializedName("html_url") val htmlUrl: String,
    val sha: String,
    val commit: Commit,
    val author: Author
) {
    class Author(@SerializedName("login") val name: String)
    class Commit(val message: String)
}
