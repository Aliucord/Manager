package com.aliucord.manager.util

import android.app.Activity
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import com.aliucord.manager.ui.screens.home.HomeScreen

/**
 * Custom back logic for handling special screens differently.
 * If on Home, Landing, or Debug then exit immediately otherwise go back one.
 */
fun Stack<Screen>.back(currentActivity: Activity?) {
    val top = this.lastItemOrNull ?: return
    val stackSize = this.items.size

    if (stackSize > 1) {
        pop()
    } else if (top is HomeScreen) {
        currentActivity?.finish()
    } else {
        replaceAll(HomeScreen())
    }
}

/**
 * Workaround for an issue where a button can push a screen with the same key twice onto the stack.
 * https://github.com/adrielcafe/voyager/issues/474#issuecomment-2907701009
 */
infix fun Stack<Screen>.pushOnce(item: Screen) {
    if (lastItemOrNull?.key != item.key) {
        push(item)
    }
}
