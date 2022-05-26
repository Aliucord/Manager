package com.aliucord.manager.installer.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class InstallService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (val statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmationIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(confirmationIntent)
            }
            PackageInstaller.STATUS_SUCCESS -> Toast.makeText(
                this,
                "Successfully installed Aliucord!",
                Toast.LENGTH_LONG
            ).show()
            PackageInstaller.STATUS_FAILURE_ABORTED -> Toast.makeText(
                this,
                "Aborted installation",
                Toast.LENGTH_LONG
            ).show()
            else -> {
                Log.i("Installer", "Installation failed: error code $statusCode")
                Toast.makeText(this, "Failed: Error $statusCode", Toast.LENGTH_LONG).show()
            }
        }

        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null
}
