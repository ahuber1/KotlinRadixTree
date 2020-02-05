package com.ahuber.utils

import java.time.Duration

data class TimedResult<T>(val duration: Duration, val result: T)

inline fun <R> measureNanoTimeWithResult(block: () -> R): TimedResult<R> {
    val start = System.nanoTime()
    val result = block()
    val nano = System.nanoTime() - start
    val duration = Duration.ofNanos(nano)
    return TimedResult(duration, result)
}

inline fun <R> measureMillisWithResult(block: () -> R): TimedResult<R> {
    val start = System.currentTimeMillis()
    val result = block()
    val nano = System.currentTimeMillis() - start
    val duration = Duration.ofMillis(nano)
    return TimedResult(duration, result)
}