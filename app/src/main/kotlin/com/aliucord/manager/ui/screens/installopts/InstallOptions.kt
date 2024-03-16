package com.aliucord.manager.ui.screens.installopts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.aliucord.manager.util.ColorParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Immutable
@Parcelize
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
) : Parcelable {
    @Immutable
    @Parcelize
    sealed interface IconReplacement : Parcelable {
        /**
         * Keeps the original icons that are present in the APK.
         */
        @Immutable
        @Parcelize
        data object Original : IconReplacement

        /**
         * Changes the background of the icon to a specific color without
         * altering the foreground or monochrome variants.
         */
        @Immutable
        @Parcelize
        data class CustomColor(
            @TypeParceler<Color, ColorParceler>
            val color: Color,
        ) : IconReplacement

        /**
         * Replaces the foreground image of the icon entirely and sets the background to transparent.
         * This does not affect the monochrome icon.
         */
        @Immutable
        @Parcelize
        data class CustomImage(val imageBytes: ByteArray) : IconReplacement {
            override fun hashCode() = imageBytes.contentHashCode()
            override fun equals(other: Any?) = this === other
                || (javaClass == other?.javaClass && imageBytes.contentEquals((other as CustomImage).imageBytes))
        }

        companion object {
            /**
             * The default icon replacement option.
             */
            val Aliucord = CustomColor(Color(0xFF00C853))
        }
    }
}
