package com.aliucord.manager.installers.shizuku

import android.content.Context
import android.content.pm.*
import android.os.Build
import dev.rikka.tools.refine.Refine
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuPMWrapper {
    // We spoof Google Play Store to prevent unnecessary checks
    private const val PLAY_PACKAGE_NAME = "com.android.vending"

    /**
     * Gets the Shizuku binder for [android.content.pm.IPackageInstaller].
     */
    fun getPackageInstallerBinder(): IPackageInstaller {
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
        val iPackageInstaller = getPackageInstallerBinder()

        val hiddenPackageInstaller = if (Build.VERSION.SDK_INT >= 31) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* installerAttributionTag = */ null,
                /* userId = */ 0,
            )
        } else if (Build.VERSION.SDK_INT >= 26) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ 0,
            )
        } else {
            PackageInstallerHidden(
                /* context = */ context,
                /* pm = */ context.packageManager,
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ 0,
            )
        }
        return Refine.unsafeCast(hiddenPackageInstaller)
    }

    /**
     * Opens and binds a [PackageInstaller.Session] wrapper through Shizuku.
     */
    fun openSession(sessionId: Int): PackageInstaller.Session {
        val iPackageInstaller = getPackageInstallerBinder()
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }
}
