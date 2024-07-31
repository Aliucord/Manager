package com.aliucord.manager.ui.screens.patchopts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.aliucord.manager.util.ColorParceler
import com.aliucord.manager.util.ColorSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Parcelize
@Serializable
data class PatchOptions(
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
     * This is independent of [PatchOptions.iconReplacement]
     */
    val monochromeIcon: Boolean,
) : Parcelable {
    @Immutable
    @Parcelize
    @Serializable
    sealed interface IconReplacement : Parcelable {
        /**
         * Keeps the original icons that are present in the APK.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("original")
        data object Original : IconReplacement

        /**
         * Changes the background of the icon to a specific color without
         * altering the foreground or monochrome variants.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("color")
        data class CustomColor(
            @TypeParceler<Color, ColorParceler>
            @Serializable(ColorSerializer::class)
            val color: Color,
        ) : IconReplacement

        /**
         * Replaces the foreground image of the icon entirely and sets the background to transparent.
         * This does not affect the monochrome icon.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("image")
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
