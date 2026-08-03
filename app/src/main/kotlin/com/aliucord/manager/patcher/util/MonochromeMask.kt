package com.aliucord.manager.patcher.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

object MonochromeMask {
    // Builds the themed layer for [imageBytes] as a PNG
    fun create(imageBytes: ByteArray): ByteArray {
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
            val mask = if (hasTransparency) alpha else luminance(pixel) * alpha / 255

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

    // Rec. 601: Perceptual brightness, the safe default for photos and most logos
    private fun luminance(pixel: Int): Int {
        val red = pixel ushr 16 and 0xFF
        val green = pixel ushr 8 and 0xFF
        val blue = pixel and 0xFF

        return (red * 299 + green * 587 + blue * 114) / 1000
    }
}
