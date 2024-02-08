package com.aliucord.manager.installers.pm

import android.content.*
import com.aliucord.manager.installers.InstallerResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PMResultReceiver(
    private val sessionId: Int,
    private val continuation: Continuation<InstallerResult>,
) : BroadcastReceiver() {
    /**
     * The intent filter this receiver should be registered with to work properly.
     */
    val filter = IntentFilter(ACTION_RECEIVE_RESULT)

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RECEIVE_RESULT) return
        if (intent.getIntExtra(EXTRA_SESSION_ID, -1) != sessionId) return

        val result = intent.getParcelableExtra<InstallerResult>(EXTRA_RESULT)
            ?: return

        continuation.resume(result)
    }

    companion object {
        const val ACTION_RECEIVE_RESULT = "com.aliucord.manager.RELAY_PM_RESULT"
        const val EXTRA_RESULT = "installerResult"
        const val EXTRA_SESSION_ID = "sessionId"
    }
}
