package com.aliucord.manager.installers.pm

import android.content.*
import android.content.pm.PackageInstaller
import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.util.showToast

class PMIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val installerResult = when (val statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            -999 -> {
                // Invalid intent
                null
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmationIntent = intent
                    .getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(confirmationIntent)

                null // no result yet
            }

            PackageInstaller.STATUS_SUCCESS -> {
                context.showToast(R.string.installer_success)
                InstallerResult.Success
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                context.showToast(R.string.installer_aborted)
                InstallerResult.Cancelled(systemTriggered = true)
            }

            else -> {
                Log.w(BuildConfig.TAG, "Install failed with error code $statusCode")

                if (statusCode <= PackageInstaller.STATUS_SUCCESS)
                    null // Unknown status code (not an error)
                else {
                    PMInstallerError(statusCode).also {
                        context.showToast(it.localizedReason)
                    }
                }
            }
        }

        if (installerResult != null) {
            val relayIntent = Intent(PMResultReceiver.ACTION_RECEIVE_RESULT)
                .putExtra(PMResultReceiver.EXTRA_RESULT, installerResult)

            context.sendBroadcast(relayIntent)
        }
    }
}
