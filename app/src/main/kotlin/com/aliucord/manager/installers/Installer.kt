package com.aliucord.manager.installers

import java.io.File

/**
 * A generic installer interface that manages installing APKs
 */
interface Installer {
    /**
     * Starts an installation and forgets about it. A toast will be shown if the installation completes successfully.
     * @param apks All APKs (including any splits) willed be merged into a single install.
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    suspend fun install(apks: List<File>, silent: Boolean = true)

    /**
     * Starts an installation and waits for it to finish with a result. A toast will be shown for all result states.
     * @param apks All APKs (including any splits) willed be merged into a single install.
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    suspend fun waitInstall(apks: List<File>, silent: Boolean = true): InstallerResult

    /**
     * Triggers an uninstallation and waits for it to complete with a result.  A toast will be shown for all result states.
     * @param packageName The package name of the target package.
     */
    suspend fun waitUninstall(packageName: String): InstallerResult
}
