package com.ahuber.utils

import java.util.*

enum class ComparisonResult {
    LESS_THAN_TARGET,
    EQUAL_TO_TARGET,
    GREATER_THAN_TARGET
}

fun <T> Iterable<T>.binarySearch(comparator: (T) -> ComparisonResult): T? {
    val list = when (this) {
        is List<T> -> this
        else -> this.toList()
    }

    if (list.isEmpty()) return null
    var indices = list.indices

    while (indices.length > 0) {
        indices = when (comparator(list[indices.middle])) {
            ComparisonResult.LESS_THAN_TARGET -> IntRange(indices.first, (indices.middle - 1).coerceAtLeast(indices.first))
            ComparisonResult.EQUAL_TO_TARGET -> return list[indices.middle]
            ComparisonResult.GREATER_THAN_TARGET -> IntRange((indices.middle + 1).coerceAtMost(indices.last), indices.last)
        }
    }

    return null
}

fun <K, V> Map<K, V>.getOrNone(key: K): Option<V> {
    return when (key in this) {
        true -> Option.Some(this.getValue(key))
        false -> Option.None()
    }
}

fun <T> Stack<T>.popOrNone(): Option<T> {
    return when (this.isEmpty()) {
        true -> Option.None()
        false -> Option.Some(this.pop())
    }
}

fun <T> LinkedList<T>.removeFirstOrNone(): Option<T> {
    return when (this.isEmpty()) {
        true -> Option.None()
        false -> Option.Some(this.removeFirst())
    }
}