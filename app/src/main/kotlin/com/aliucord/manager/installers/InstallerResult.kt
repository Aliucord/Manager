package com.aliucord.manager.installers

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * The state of an APK installation after it has completed and cleaned up.
 */
sealed interface InstallerResult : Parcelable {
    /**
     * The installation was successfully completed.
     */
    @Parcelize
    data object Success : InstallerResult

    /**
     * This installation was interrupted and the install session has been canceled.
     * @param systemTriggered Whether the cancellation happened from the system (ie. clicked cancel on the install prompt)
     *                        Otherwise, this was caused by a coroutine cancellation.
     */
    @Parcelize
    data class Cancelled(val systemTriggered: Boolean) : InstallerResult

    /**
     * This installation encountered an error and has been aborted.
     * All implementors should implement [Parcelable].
     */
    abstract class Error : InstallerResult {
        /**
         * Loggable error that should not be shown to the user.
         */
        abstract val debugReason: String

        /**
         * Simplified + translatable user facing errors.
         */
        @get:StringRes
        abstract val localizedReason: Int
    }
}
