package com.aliucord.manager.installers

import java.io.File

/**
 * A generic installer interface that manages installing APKs
 */
interface Installer {
    /**
     * Starts an installation and forgets about it. A toast will be shown when the installation was completed.
     * @param apks All APKs (including any splits) to merge into a single install.
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    fun install(apks: List<File>, silent: Boolean = true)

    /**
     * Starts an installation and waits for it to finish with a result. A toast will be shown when the installation was completed.
     * @param apks All APKs (including any splits) to merge into a single install.
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    suspend fun waitInstall(apks: List<File>, silent: Boolean = true): InstallerResult
}
