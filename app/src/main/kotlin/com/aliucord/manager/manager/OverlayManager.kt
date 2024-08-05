package com.aliucord.manager.manager

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.aliucord.manager.di.ActivityProvider
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.components.Theme
import kotlinx.coroutines.*
import kotlin.coroutines.resume

typealias ResultComposable<R> = @Composable (onResult: (R) -> Unit) -> Unit

/**
 * This is used to display dialogs on top of the current activity at any point in the code.
 * The main use case for this is dialogs for which a result is needed during patching steps.
 * The only other alternative is binding `Flow`s from the steps back to the patching screen model
 * and then displaying them in the UI, which is too much boilerplate.
 */
class OverlayManager(
    private val prefs: PreferencesManager,
    private val activityProvider: ActivityProvider,
) {
    /**
     * Attaches a new [ComposeView] content view to the root of the activity on top of everything else.
     * This view will be displayed until the `onResult` callback is called,
     * after which this method will finish suspending with the result from the callback invoke.
     */
    suspend fun <R> startComposableForResult(newDialog: ResultComposable<R>): R {
        return suspendCancellableCoroutine { continuation ->
            val activity = activityProvider.get<Activity>()
            var view: View? = null

            val callback = callback@{ value: R ->
                val v = view ?: return@callback

                CoroutineScope(Dispatchers.Main).launch {
                    (v.parent as ViewGroup).removeView(v)
                    view = null
                }

                continuation.resume(value)
            }

            continuation.invokeOnCancellation {
                CoroutineScope(Dispatchers.Main).launch {
                    val v = view ?: return@launch

                    (v.parent as ViewGroup).removeView(v)
                    view = null
                }
            }

            view = ComposeView(activity).apply {
                setContent {
                    ManagerTheme(
                        isDarkTheme = prefs.theme == Theme.DARK || prefs.theme == Theme.SYSTEM && isSystemInDarkTheme(),
                        isDynamicColor = prefs.dynamicColor
                    ) {
                        newDialog(callback)
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                activity.addContentView(
                    view!!,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )

                view?.bringToFront()
            }
        }
    }
}
