package com.aliucord.manager.patcher.steps

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aliucord.manager.R
import kotlinx.parcelize.Parcelize

/**
 * A group of steps that is shown under one section in the patching UI.
 * This has no functional impact other than organization.
 */
@Immutable
@Parcelize
enum class StepGroup(
    /**
     * The UI name to display this group as
     */
    @get:StringRes
    val localizedName: Int,
) : Parcelable {
    Prepare(R.string.install_group_prepare),
    Download(R.string.install_group_download),
    Patch(R.string.install_group_patch),
    Install(R.string.install_group_install)
}
