package com.aliucord.manager.installers.root

import android.content.Context
import com.aliucord.manager.installers.*
import com.aliucord.manager.util.getUserId
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import java.io.File

// Based on https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/RootInstaller.kt

/**
 * Installer based on using libsu to invoke `pm` with root.
 *
 * Errors from this installer will always be [UnknownInstallerError]
 * as it is impossible to extract meaningful information from shell installations.
 */
class RootInstaller(private val context: Context) : Installer {
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun executeSU(command: String): List<String> = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        if (result.code != 0) {
            val resultString = "Result code: ${result.code}. Stdout: '${result.out}'. Stderr: '${result.err}'."
            val message = "Root command '$command' failed. $resultString"
            throw ShellException(message)
        }
        result.out
    }

    /**
     * Creates the main root shell and requests root permissions.
     * If they are not granted, throw an exception.
     */
    private fun obtainRoot() {
        Shell.getShell().waitAndClose()
        Shell.getShell()

        if (Shell.isAppGrantedRoot() != true)
            throw ShellException("Missing root permissions (denied)")
    }

    private suspend fun createInstallSession(totalSize: Long): Int {
        val userId = context.getUserId()?.toString() ?: "all"
        val response = executeSU("pm install-create -i $PLAY_PACKAGE_NAME --user $userId -r -S $totalSize")
        val result = response[0]

        val sessionIdMatch = Regex("""\d+""").find(result)
        checkNotNull(sessionIdMatch) { "Can't find session id with regex pattern. Output: $result" }

        val sessionId = sessionIdMatch.groups[0]
        checkNotNull(sessionId) { "Can't find match group containing the session id. Output: $result" }

        return sessionId.value.toInt()
    }

    /**
     * Disable ADB install verification (bypass useless Play Protect).
     */
    private suspend fun disableAdbVerify() {
        executeSU("settings put global verifier_verify_adb_installs 0")
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        coroutineScope.launch { waitInstall(apks, silent) }
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        val invalidChars = """\W""".toRegex()
        for (apk in apks) {
            if (hasDangerousCharacter(apk.canonicalPath) || hasDangerousCharacter(apk.name))
                throw IllegalArgumentException("APK path or name has dangerous characters: ${apk.canonicalPath}")
            if (apk.nameWithoutExtension.contains(invalidChars))
                throw IllegalArgumentException("APK file name contains invalid characters: ${apk.nameWithoutExtension}")
        }

        obtainRoot()
        disableAdbVerify()

        val sessionId = createInstallSession(
            totalSize = apks.sumOf(File::length),
        )

        return try {
            for (apk in apks) {
                executeSU("""cat "${apk.canonicalPath}" | pm install-write -S ${apk.length()} $sessionId "${apk.name}"""")
            }
            executeSU("""pm install-commit $sessionId""")

            InstallerResult.Success
        } catch (t: Throwable) {
            executeSU("""pm install-abandon $sessionId""")

            UnknownInstallerError(t)
        }
    }

    override suspend fun waitUninstall(packageName: String): InstallerResult {
        if (hasDangerousCharacter(packageName))
            throw IllegalArgumentException("packageName has dangerous characters!")

        obtainRoot()

        return try {
            val userFlag = context.getUserId()?.let { "--user $it" } ?: ""

            executeSU("""pm uninstall $userFlag $packageName""")
            InstallerResult.Success
        } catch (t: Throwable) {
            UnknownInstallerError(t)
        }
    }

    private companion object {
        // We spoof Google Play Store to prevent unnecessary checks
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        /**
         * Checks if [value] has a dangerous character to put in a shell.
         * Paths and names should be checked with this.
         */
        fun hasDangerousCharacter(value: String): Boolean = dangerousCharacters.containsMatchIn(value)

        private val dangerousCharacters = """[`'()|<>*$&?!#:;{}\s"\[\]\\]""".toRegex()
    }
}

private class ShellException(message: String) : Exception(message)
