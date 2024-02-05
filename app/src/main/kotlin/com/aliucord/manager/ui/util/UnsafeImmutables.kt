@file:Suppress("unused")

package com.aliucord.manager.ui.util

import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.adapters.*

/*
 * Compose-stable wrappers over a list for performance.
 *
 * This does NOT guarantee stability. It is merely a stable wrapper over another collection,
 * and assumes the user knows that it shouldn't change through crucial parts of rendering.
 */

fun <T> Collection<T>.toUnsafeImmutable(): ImmutableCollection<T> =
    ImmutableCollectionAdapter(this)

fun <T> List<T>.toUnsafeImmutable(): ImmutableList<T> =
    ImmutableListAdapter(this)

fun <T> Set<T>.toUnsafeImmutable(): ImmutableSet<T> =
    ImmutableSetAdapter(this)

fun <K, V> Map<K, V>.toUnsafeImmutable(): ImmutableMap<K, V> =
    ImmutableMapAdapter(this)

fun <T> emptyImmutableList(): ImmutableList<T> = persistentListOf()
fun <T> emptyImmutableSet(): ImmutableSet<T> = persistentSetOf()
fun <K, V> emptyImmutableMap(): ImmutableMap<K, V> = persistentMapOf()
