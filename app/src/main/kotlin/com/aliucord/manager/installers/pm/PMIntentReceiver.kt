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
        val realSessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val expectedSessionId = intent.getIntExtra(EXTRA_SESSION_ID, -2)

        if (realSessionId != expectedSessionId) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        val installerResult = when (status) {
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
                Log.w(BuildConfig.TAG, "Install failed with error code $status")

                if (status <= PackageInstaller.STATUS_SUCCESS)
                    null // Unknown status code (not an error)
                else {
                    PMInstallerError(status).also {
                        context.showToast(it.localizedReason)
                    }
                }
            }
        }

        // Forward result to PMResultReceiver if relaying is enabled and have a real result
        if (installerResult != null && intent.getBooleanExtra(EXTRA_RELAY_ENABLED, false)) {
            val relayIntent = Intent(PMResultReceiver.ACTION_RECEIVE_RESULT)
                .putExtra(PMResultReceiver.EXTRA_RESULT, installerResult)
                .putExtra(PMResultReceiver.EXTRA_SESSION_ID, realSessionId)

            context.sendBroadcast(relayIntent)
        }
    }

    companion object {
        const val EXTRA_RELAY_ENABLED = "relayEnabled"
        const val EXTRA_SESSION_ID = "expectedSessionId"
    }
}
