package com.aliucord.manager.ui.util

import android.os.Parcelable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

// TODO: migrate to androidx-nav v3 since it has built-in screen result support
/**
 * Global result registry for managing screen results across the app.
 * This is a hacky workaround for the lack of a proper screen result system in Voyager navigation.
 * https://github.com/adrielcafe/voyager/issues/465#issuecomment-2696523168
 */
private object ScreenResultRegistry {
    private val mutex = Mutex()
    private val results = HashMap<String, CompletableDeferred<Any?>>()

    suspend fun <R> registerResult(key: String): CompletableDeferred<R> {
        val deferred = CompletableDeferred<Any?>()
        mutex.withLock {
            results[key] = deferred
        }
        @Suppress("UNCHECKED_CAST")
        return deferred as CompletableDeferred<R>
    }

    suspend fun setResult(key: String, result: Any?) {
        mutex.withLock {
            results[key]?.complete(result)
            results.remove(key)
        }
    }

    suspend fun clear(key: String) {
        mutex.withLock {
            results[key]?.cancel()
            results.remove(key)
        }
    }
}

/**
 * Base class for screens that can return results
 */
abstract class ScreenWithResult<R> : Screen {
    val resultKey: ScreenResultKey = ScreenResultKey()

    /**
     * This sets the result for this screen and completes any listeners.
     */
    protected suspend fun setResult(value: R) {
        ScreenResultRegistry.setResult(resultKey.key, value)
    }
}

/**
 * Base class for screen models that can set a result for their parent screen.
 */
abstract class ScreenModelWithResult<R>(
    private val resultKey: ScreenResultKey,
) : ScreenModel {
    /**
     * This sets the result for this screen and completes any listeners.
     * Preferably, this should be called in [ScreenModel.onDispose]
     */
    protected suspend fun setResult(value: R) {
        ScreenResultRegistry.setResult(resultKey.key, value)
    }
}

@Parcelize
@Serializable
data class ScreenResultKey(val key: String = UUID.randomUUID().toString()) : Parcelable

/**
 * Extension function to show a screen and get its result from anywhere
 */
suspend fun <R> Navigator.pushForResult(
    screen: ScreenWithResult<R>,
): R {
    val deferred = ScreenResultRegistry.registerResult<R>(screen.resultKey.key)
    this.push(screen)
    return try {
        deferred.await()
    } finally {
        ScreenResultRegistry.clear(screen.resultKey.key)
    }
}

/**
 * Extension function to show a screen without waiting for the result
 */
suspend fun <R> Navigator.showWithoutWaiting(
    screen: ScreenWithResult<R>,
): ScreenResultKey {
    ScreenResultRegistry.registerResult<R>(screen.resultKey.key)
    this.push(screen)
    return screen.resultKey
}

/**
 * Function to get a result using the key from a previous [showWithoutWaiting] call
 */
suspend fun <R> getScreenResult(key: ScreenResultKey): R {
    val deferred = ScreenResultRegistry.registerResult<R>(key.key)
    return deferred.await()
}
