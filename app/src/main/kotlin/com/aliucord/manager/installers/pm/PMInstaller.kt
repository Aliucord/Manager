package com.aliucord.manager.installers.pm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.Process
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

/**
 * APK installer using the [PackageInstaller] from the system's [PackageManager] service.
 */
class PMInstaller(
    val context: Context,
) : Installer {
    override fun install(apks: List<File>, silent: Boolean) {
        val packageInstaller = context.packageManager.packageInstaller

        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)

            if (Build.VERSION.SDK_INT >= 24) {
                setOriginatingUid(Process.myUid())
            }

            if (Build.VERSION.SDK_INT >= 26) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }

            if (Build.VERSION.SDK_INT >= 31) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)

                if (silent) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            if (Build.VERSION.SDK_INT >= 34) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
            }
        }

        val callbackIntent = Intent(context, PMIntentReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE,
        )

        val sessionId = packageInstaller.createSession(params)

        packageInstaller.openSession(sessionId).use { session ->
            val bufferSize = 1 * 1024 * 1024 // 1MiB

            for (apk in apks) {
                session.openWrite(apk.name, 0, apk.length()).use { outStream ->
                    apk.inputStream().use { it.copyTo(outStream, bufferSize) }
                    session.fsync(outStream)
                }
            }

            session.commit(pendingIntent.intentSender)
        }
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        return suspendCancellableCoroutine { continuation ->
            // This will receive parsed data forwarded by PMIntentReceiver
            val relayReceiver = PMResultReceiver(continuation)

            try {
                @SuppressLint("UnspecifiedRegisterReceiverFlag")
                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(relayReceiver, relayReceiver.filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(relayReceiver, relayReceiver.filter)
                }
            } finally {
                context.unregisterReceiver(relayReceiver)
            }
        }
    }
}
