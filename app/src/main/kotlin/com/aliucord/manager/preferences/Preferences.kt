/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.preferences

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlin.reflect.KProperty

var sharedPreferences: SharedPreferences? = null

class Preference<T>(
    private val key: String,
    defaultValue: T,
    getter: SharedPreferences.(key: String, defaultValue: T) -> T?,
    private val setter: SharedPreferences.Editor.(key: String, newValue: T) -> Unit
) {
    val value = mutableStateOf(sharedPreferences!!.getter(key, defaultValue) ?: defaultValue)

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value.value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) = set(newValue)

    fun get() = value.value

    fun set(newValue: T) {
        if (value.value == newValue) return

        value.value = newValue

        sharedPreferences!!.edit { setter(key, newValue) }
    }
}

fun boolPreference(
    key: String,
    defaultValue: Boolean = false
) = Preference(
    key,
    defaultValue,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean
)

fun intPreference(
    key: String,
    defaultValue: Int = 0
) = Preference(
    key,
    defaultValue,
    SharedPreferences::getInt,
    SharedPreferences.Editor::putInt
)

fun stringPreference(
    key: String,
    defaultValue: String
) = Preference(
    key,
    defaultValue,
    SharedPreferences::getString,
    SharedPreferences.Editor::putString
)