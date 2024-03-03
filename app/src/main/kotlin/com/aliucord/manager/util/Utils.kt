package com.aliucord.manager.util

import androidx.collection.ObjectList

inline fun <E> ObjectList<E>.find(block: (E) -> Boolean): E? {
    forEach { value ->
        if (block(value))
            return value
    }

    return null
}
