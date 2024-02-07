package com.aliucord.manager.installer.steps

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aliucord.manager.R

/**
 * A group of steps that is shown under one section in the install UI.
 * This has no functional impact.
 */
@Immutable
enum class StepGroup(
    /**
     * The UI name to display this group as
     */
    @get:StringRes
    val localizedName: Int,
) {
    Prepare(R.string.install_group_prepare),
    Download(R.string.install_group_download),
    Patch(R.string.install_group_patch),
    Install(R.string.install_group_install)
}
