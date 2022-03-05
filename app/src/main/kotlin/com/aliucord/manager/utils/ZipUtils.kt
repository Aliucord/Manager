package com.aliucord.manager.utils

import com.aliucord.libzip.Zip
import java.io.InputStream

fun Zip.writeEntry(entryName: String, stream: InputStream) = writeEntry(entryName, stream.readBytes())
fun Zip.writeEntry(entryName: String, bytes: ByteArray) {
    openEntry(entryName)
    writeEntry(bytes, bytes.size.toLong())
    closeEntry()
}