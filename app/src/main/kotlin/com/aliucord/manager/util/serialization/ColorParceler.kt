package com.aliucord.manager.util.serialization

import android.os.Parcel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.parcelize.Parceler

/**
 * Parcelize Compose [Color] for reading and writing in ARGB representation for the sRGB color space.
 */
object ColorParceler : Parceler<Color> {
    override fun create(parcel: Parcel): Color =
        Color(parcel.readInt())

    override fun Color.write(parcel: Parcel, flags: Int) =
        parcel.writeInt(toArgb())
}
