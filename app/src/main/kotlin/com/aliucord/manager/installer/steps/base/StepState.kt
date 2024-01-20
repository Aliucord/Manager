package com.aliucord.manager.installer.steps.base

enum class StepState {
    Pending,
    Running,
    Success,
    Error,
    Skipped,
    Cancelled, // TODO: something like the discord dnd sign except its not red, but gray maybe
}
