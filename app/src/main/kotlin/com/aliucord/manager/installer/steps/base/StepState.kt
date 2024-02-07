package com.aliucord.manager.installer.steps.base

enum class StepState {
    Pending,
    Running,
    Success,
    Error,
    Skipped;

    val isFinished: Boolean
        get() = this == Success || this == Error || this == Skipped
}
