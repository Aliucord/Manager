/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.installer.util

import com.aliucord.libzip.Zip
import java.io.InputStream

fun Zip.writeEntry(entryName: String, stream: InputStream) = writeEntry(entryName, stream.readBytes())
fun Zip.writeEntry(entryName: String, bytes: ByteArray) {
    openEntry(entryName)
    writeEntry(bytes, bytes.size.toLong())
    closeEntry()
}
