package com.aliucord.manager.util

import android.content.*
import android.os.Environment
import android.widget.Toast
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import java.io.File

fun Context.copyText(text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText(BuildConfig.APPLICATION_ID, text)
    )
}

fun Context.saveFile(name: String, text: String): File? {
    val file = File(Environment.getExternalStorageDirectory(), "/Download/$name")

    return try {
        file.writeText(text)
        Toast.makeText(this, getString(R.string.installer_file_save_success, file.absolutePath), Toast.LENGTH_SHORT)
        file
    } catch (e: Throwable) {
        Toast.makeText(this, getString(R.string.installer_file_save_failed, file.absolutePath), Toast.LENGTH_SHORT)
        null
    }
}
