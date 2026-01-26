package com.aliucord.manager.ui.screens.componentopts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.time.Instant

/**
 * A custom component that was deployed to this device with
 * the `deployWithAdb` task and imported by Manager.
 */
@Immutable
@Parcelize
@Serializable
data class PatchComponent(
    /**
     * The type of this custom component.
     */
    val type: Type,
    /**
     * The build version of this custom component.
     */
    val version: SemVer,
    /**
     * The time at which this custom component was deployed to the device and imported by manager.
     */
    val timestamp: Instant,
) : Parcelable {
    @Parcelize
    @Serializable
    enum class Type : Parcelable {
        @SerialName("injector")
        Injector,

        @SerialName("patches")
        Patches,
    }

    /**
     * Returns the imported file where this custom component should be stored.
     * This is not guaranteed to exist.
     */
    fun getFile(paths: PathManager): File {
        val dir = when (type) {
            Type.Injector -> paths.customInjectorsDir
            Type.Patches -> paths.customPatchesDir
        }
        val ext = when (type) {
            Type.Injector -> "dex"
            Type.Patches -> "zip"
        }

        // ${timestamp}_${componentVersion}.${componentFile.extension}
        return dir.resolve("${timestamp.toEpochMilliseconds()}_$version.$ext")
    }
}
