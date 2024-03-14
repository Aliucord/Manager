package com.aliucord.manager.ui.screens.installopts

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class InstallOptions(
    /**
     * The app name that's user-facing in launchers.
     */
    val appName: String,

    /**
     * Changes the installation package name.
     */
    val packageName: String,

    /**
     * Adding the debuggable APK flag.
     */
    val debuggable: Boolean,

    /**
     * Replacement of the user-facing launcher icon.
     */
    val iconReplacement: IconReplacement,

    /**
     * Adding a monochrome variant to the launcher icon.
     * This is independent of [InstallOptions.iconReplacement]
     */
    val monochromeIcon: Boolean,
) {
    @Immutable
    sealed interface IconReplacement {
        /**
         * Keeps the original icons that are present in the APK.
         */
        data object Original : IconReplacement

        /**
         * Replaces the foreground image of the icon entirely and sets the background to transparent.
         * This does not affect the monochrome icon.
         */
        data class CustomImage(val imageBytes: ByteArray) : IconReplacement

        /**
         * Changes the background of the icon to a specific color without
         * altering the foreground or monochrome variants.
         */
        data class CustomColor(val color: Color) : IconReplacement

        companion object {
            /**
             * The default icon replacement option.
             */
            val Aliucord = CustomColor(Color(0xFF00C853))
        }
    }
}
