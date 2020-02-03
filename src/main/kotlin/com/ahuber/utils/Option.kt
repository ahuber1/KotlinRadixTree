package com.ahuber.utils

sealed class Option<T> {
    data class Some<T>(val value: T): Option<T>()
    class None<T>: Option<T>()
}

fun <T> Option<T>.nullIfNone() = this.mapIfNone { null }

inline fun <T : R, R> Option<T>.mapIfNone(block: () -> R) : R {
    return when (this) {
        is Option.Some -> this.value
        is Option.None -> block()
    }
}



