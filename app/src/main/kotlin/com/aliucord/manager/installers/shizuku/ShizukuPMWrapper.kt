package com.aliucord.manager.installers.shizuku

import android.content.Context
import android.content.pm.*
import android.os.Build
import com.aliucord.manager.util.HiddenAPI
import com.aliucord.manager.util.getUserId
import dev.rikka.tools.refine.Refine
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuPMWrapper {
    // We spoof Google Play Store to prevent unnecessary checks
    private const val PLAY_PACKAGE_NAME = "com.android.vending"

    /**
     * Gets the Shizuku binder for [android.content.pm.IPackageInstaller].
     */
    private fun getPackageInstallerBinder(): IPackageInstaller {
        val iPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        val iPackageInstaller = IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )

        return iPackageInstaller
    }

    /**
     * Gets a binded [android.content.pm.PackageInstaller] service wrapper through Shizuku.
     */
    fun getPackageInstaller(context: Context): PackageInstaller {
        HiddenAPI.disable()

        val userId = context.getUserId() ?: 0
        val iPackageInstaller = getPackageInstallerBinder()

        val hiddenPackageInstaller = if (Build.VERSION.SDK_INT >= 31) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* installerAttributionTag = */ null,
                /* userId = */ userId,
            )
        } else if (Build.VERSION.SDK_INT >= 26) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ userId,
            )
        } else {
            PackageInstallerHidden(
                /* context = */ context,
                /* pm = */ context.packageManager,
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ userId,
            )
        }
        return Refine.unsafeCast(hiddenPackageInstaller)
    }

    /**
     * Opens and binds a [PackageInstaller.Session] wrapper through Shizuku.
     */
    fun openSession(sessionId: Int): PackageInstaller.Session {
        HiddenAPI.disable()

        val iPackageInstaller = getPackageInstallerBinder()
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }
}
