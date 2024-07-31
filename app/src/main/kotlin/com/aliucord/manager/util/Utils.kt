package com.aliucord.manager.util

import androidx.collection.ObjectList
import com.aliucord.manager.BuildConfig
import kotlin.math.pow
import kotlin.math.truncate

/**
 * Truncates this value to a specific number of [decimals] digits.
 */
fun Double.toPrecision(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return truncate(this * multiplier) / multiplier
}

inline fun <E> ObjectList<E>.find(block: (E) -> Boolean): E? {
    forEach { value ->
        if (block(value))
            return value
    }

    return null
}

/**
 * Whether this manager build is not an official release.
 */
@Suppress("KotlinConstantConditions")
val IS_CUSTOM_BUILD by lazy {
    BuildConfig.GIT_BRANCH != "release"
        || BuildConfig.GIT_LOCAL_CHANGES
        || BuildConfig.GIT_LOCAL_COMMITS
}
