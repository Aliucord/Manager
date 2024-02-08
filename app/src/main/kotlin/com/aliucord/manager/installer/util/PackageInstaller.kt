package com.aliucord.manager.installer.util

import android.content.Context
import android.content.Intent
import android.net.Uri

// TODO: move this to PMInstaller
fun Context.uninstallApk(packageName: String) {
    val packageURI = Uri.parse("package:$packageName")
    val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    startActivity(uninstallIntent)
}
