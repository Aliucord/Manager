package com.aliucord.manager.patcher.util

/**
 * Used to indicate that pre-allocating storage space via [android.os.storage.StorageManager.allocateBytes]
 * failed due to insufficient storage space, or cache space able to be cleared.
 */
class InsufficientStorageException(
    message: String?,
) : Exception() {
    override val message = "Failed to preallocate sufficient storage space: $message"
}
