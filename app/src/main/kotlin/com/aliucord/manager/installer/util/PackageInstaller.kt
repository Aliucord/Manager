package com.aliucord.manager.installer.util

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import com.aliucord.manager.installer.service.InstallService
import java.io.File

fun Application.installApks(vararg apks: File) =
    packageManager.packageInstaller.installApks(this, *apks)

fun PackageInstaller.installApks(application: Application, vararg apks: File) {
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val sessionId = createSession(params)
    val session = openSession(sessionId)

    apks.forEach { apk ->
        session.openWrite(apk.name, 0, apk.length()).use {
            it.write(apk.readBytes())
            session.fsync(it)
        }
    }

    val callbackIntent = Intent(application, InstallService::class.java)

    @SuppressLint("UnspecifiedImmutableFlag")
    val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getService(application, 0, callbackIntent, PendingIntent.FLAG_MUTABLE)
    } else {
        PendingIntent.getService(application, 0, callbackIntent, 0)
    }

    session.commit(contentIntent.intentSender)
    session.close()
}

fun Context.uninstallApk(packageName: String) {
    val packageURI = Uri.parse("package:$packageName")
    val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    startActivity(uninstallIntent)
}
