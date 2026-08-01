package com.aliucord.manager.patcher.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.core.graphics.createBitmap
import com.aliucord.manager.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream

@Immutable
@Serializable
enum class MonochromeWeighting(@StringRes val labelRes: Int) {
    // Perceptual brightness, the safe default for photos and most logos
    @SerialName("rec601")
    Rec601(R.string.iconopts_themed_rec601) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            (red * 299 + green * 587 + blue * 114) / 1000
    },

    // Regular sRGB brightness, even more green-weighted than [Rec601]
    @SerialName("rec709")
    Rec709(R.string.iconopts_themed_rec709) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            (red * 2126 + green * 7152 + blue * 722) / 10000
    },

    // Ignores perception, so blues stay far brighter than they look
    @SerialName("average")
    Average(R.string.iconopts_themed_average) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            (red + green + blue) / 3
    },

    // Any saturated color comes out fully opaque, which keeps flat vivid logos solid
    @SerialName("hsv_value")
    HsvValue(R.string.iconopts_themed_hsv_value) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            maxOf(red, green, blue)
    },

    // Midway between the brightest and darkest channel, washing out saturated areas
    @SerialName("hsl_lightness")
    HslLightness(R.string.iconopts_themed_hsl_lightness) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            (maxOf(red, green, blue) + minOf(red, green, blue)) / 2
    },

    // [Rec601] flipped
    @SerialName("inverted")
    Inverted(R.string.iconopts_themed_inverted) {
        override fun luminance(red: Int, green: Int, blue: Int) =
            255 - (red * 299 + green * 587 + blue * 114) / 1000
    };

    // Brightness of a single pixel (0-255), which becomes that pixel's alpha in the mask
    protected abstract fun luminance(red: Int, green: Int, blue: Int): Int

    // Builds the themed layer for [imageBytes] as a PNG
    fun createMask(imageBytes: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: error("Custom icon could not be decoded")

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        source.recycle()

        val hasTransparency = pixels.any { (it ushr 24 and 0xFF) < /* transparency level = */ 250 }

        pixels.forEachIndexed { i, pixel ->
            val alpha = pixel ushr 24 and 0xFF
            val mask = if (hasTransparency) {
                alpha
            } else {
                luminance(pixel ushr 16 and 0xFF, pixel ushr 8 and 0xFF, pixel and 0xFF) * alpha / 255
            }

            // Only replace the colors depending on alpha
            pixels[i] = mask shl 24 or 0xFFFFFF
        }

        val monochrome = createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }

        return ByteArrayOutputStream().use {
            monochrome.compress(Bitmap.CompressFormat.PNG, 100, it)
            monochrome.recycle()
            it.toByteArray()
        }
    }
}
