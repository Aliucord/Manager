package com.aliucord.manager.installer.steps.patch

import android.content.Context
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Stable
class ReplaceIconStep : Step(), KoinComponent {
    val context: Context by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.setting_replace_icon

    override suspend fun execute(container: StepContainer) {
        TODO("Not yet implemented")
    }
}
