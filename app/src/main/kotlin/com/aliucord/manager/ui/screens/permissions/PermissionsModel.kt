package com.aliucord.manager.ui.screens.permissions

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.di.ActivityProvider
import com.aliucord.manager.util.*
import java.util.UUID

class PermissionsModel(
    private val application: Application,
    private val activities: ActivityProvider,
) : ViewModel() {
    var storagePermsGranted by mutableStateOf(false)
        private set
    var installPermsGranted by mutableStateOf(false)
        private set
    var notificationsPermsGranted by mutableStateOf(false)
        private set
    var batteryPermsGranted by mutableStateOf(false)
        private set

    val requiredPermsGranted by derivedStateOf {
        storagePermsGranted && installPermsGranted
    }

    fun requestInstallPerms() {
        if (Build.VERSION.SDK_INT < 26) return

        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData("package:${BuildConfig.APPLICATION_ID}".toUri())
            .let(activities.get<Activity>()::startActivity)
    }

    fun requestStoragePerms() = permissionRequestLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestManageStoragePerms() {
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData("package:${BuildConfig.APPLICATION_ID}".toUri())
            .let(activities.get<Activity>()::startActivity) // TODO: do this for all other intent launches
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationsPerms() = permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantBatteryPerms() = application.requestNoBatteryOptimizations()

    fun refresh() = viewModelScope.launchBlock {
        storagePermsGranted = if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            application.selfHasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        installPermsGranted = Build.VERSION.SDK_INT < 26 || application.packageManager.canRequestPackageInstalls()
        notificationsPermsGranted = Build.VERSION.SDK_INT < 33 || application.selfHasPermission(Manifest.permission.POST_NOTIFICATIONS)
        batteryPermsGranted = Build.VERSION.SDK_INT < 24 || application.isIgnoringBatteryOptimizations()
    }

    init {
        refresh()
    }

    override fun onCleared() {
        permissionRequestLauncher.unregister()
    }

    /**
     * Used for requesting permissions that launch a popup to the user.
     * Refreshes the permissions state once returned to the app.
     */
    private val permissionRequestLauncher = run {
        val activity = activities.get<ComponentActivity>()

        activity.activityResultRegistry.register(
            key = UUID.randomUUID().toString(),
            contract = ActivityResultContracts.RequestPermission(),
            callback = { println("here"); refresh() },
        )
    }
}
