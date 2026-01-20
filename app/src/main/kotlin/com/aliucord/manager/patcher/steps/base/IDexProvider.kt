package com.aliucord.manager.patcher.steps.base

import com.aliucord.manager.patcher.StepRunner

interface IDexProvider {
    /**
     * The priority of the .dex files supplied by [getDexFiles].
     * Higher number leads to a higher overwrite priority.
     * .dex files already included in the APK have a priority of `0`.
     */
    val dexPriority: Int

    /**
     * The amount of files returned by [getDexFiles]
     */
    val dexCount: Int

    /**
     * Any dex files to be added into the APK.
     */
    fun getDexFiles(container: StepRunner): List<ByteArray>
}
