package com.aliucord.manager.utils

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import java.io.File

val aliucordDir = File(Environment.getExternalStorageDirectory(), "Aliucord")
val pluginsDir = File(aliucordDir, "plugins")

val gson = Gson()