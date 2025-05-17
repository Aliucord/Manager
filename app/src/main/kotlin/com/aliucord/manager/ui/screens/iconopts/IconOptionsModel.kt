package com.aliucord.manager.ui.screens.iconopts

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement
import com.aliucord.manager.util.launchBlock
import com.aliucord.manager.util.showToast
import dev.zt64.compose.pipette.HsvColor
import java.io.IOException

class IconOptionsModel(
    prefilledOptions: IconReplacement,
    private val application: Application,
) : ScreenModel {
    // ---------- Icon patching mode ---------- //
    var mode by mutableStateOf(IconOptionsMode.Original)
        private set

    fun changeMode(mode: IconOptionsMode) {
        this.mode = mode
    }

    // ---------- Replacement color ---------- //
    var selectedColor by mutableStateOf(HsvColor(IconReplacement.Aliucord.color))
        private set

    fun changeSelectedColor(color: HsvColor) {
        selectedColor = color
    }

    private fun initSelectedColor(color: Color) {
        selectedColor = HsvColor(color)
    }

    // ---------- Replacement color ---------- //
    var selectedImage by mutableStateOf<ByteArray?>(null)

    fun changeSelectedImageUri(uri: Uri) = screenModelScope.launchBlock {
        try {
            // Check file size first
            val query = application.contentResolver.query(uri, null, null, null, null)
                ?: throw IOException("Failed to query selected image uri")

            val size = query.use { cursor ->
                cursor.moveToFirst()
                cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
            }

            if (size > 1024 * 256) { // 256KiB
                application.showToast(R.string.iconopts_failed_image_too_big)
                return@launchBlock
            }

            // Read file bytes
            val bytes = application.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IOException("Failed to open input stream")

            selectedImage = bytes
        } catch (t: Throwable) {
            application.showToast(R.string.iconopts_failed_image)
            Log.w(BuildConfig.TAG, "Failed to open selected foreground replacement image", t)
        }
    }

    // ---------- Other ---------- //
    init {
        when (prefilledOptions) {
            is IconReplacement.CustomColor if prefilledOptions.color == IconReplacement.Aliucord.color -> {
                changeMode(IconOptionsMode.Aliucord)
                initSelectedColor(IconReplacement.Aliucord.color)
            }

            is IconReplacement.CustomColor -> {
                changeMode(IconOptionsMode.CustomColor)
                initSelectedColor(prefilledOptions.color)
            }

            is IconReplacement.CustomImage -> {
                changeMode(IconOptionsMode.CustomImage)
                selectedImage = prefilledOptions.imageBytes
            }

            IconReplacement.Original -> changeMode(IconOptionsMode.Original)
        }
    }

    fun generateConfig(): IconReplacement = when (mode) {
        IconOptionsMode.Original -> IconReplacement.Original
        IconOptionsMode.Aliucord -> IconReplacement.Aliucord
        IconOptionsMode.CustomColor -> IconReplacement.CustomColor(color = selectedColor.toColor())
        IconOptionsMode.CustomImage -> IconReplacement.CustomImage(
            imageBytes = selectedImage ?: throw IllegalStateException("Cannot generate config without a selected image"),
        )
    }
}
