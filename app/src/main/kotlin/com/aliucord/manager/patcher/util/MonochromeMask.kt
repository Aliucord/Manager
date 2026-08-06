package com.aliucord.manager.patcher.util

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

object MonochromeMask {
    /**
     * Create a monochrome adaptive icon layer for a specific image.
     */
    fun create(image: Bitmap): ByteArray {
        val width = image.width
        val height = image.height

        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        val hasTransparency = pixels.any { (it ushr 24 and 0xFF) < /* transparency level = */ 250 }

        pixels.forEachIndexed { i, pixel ->
            val alpha = pixel ushr 24 and 0xFF
            val mask = if (hasTransparency) {
                alpha
            } else {
                luminance(pixel) * alpha / 255
            }

            // Only replace the colors depending on alpha
            pixels[i] = (mask shl 24) or 0xFFFFFF
        }

        val newImage = createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }

        return ByteArrayOutputStream().use {
            newImage.compress(Bitmap.CompressFormat.PNG, 100, it)
            newImage.recycle()
            it.toByteArray()
        }
    }

    // Rec. 601: Perceptual brightness, the safe default for photos and most logos
    private fun luminance(pixel: Int): Int {
        val red = (pixel ushr 16) and 0xFF
        val green = (pixel ushr 8) and 0xFF
        val blue = pixel and 0xFF

        return (red * 299 + green * 587 + blue * 114) / 1000
    }
}
