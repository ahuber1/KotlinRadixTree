package com.ahuber.utils

import java.util.*

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