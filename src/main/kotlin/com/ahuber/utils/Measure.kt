package com.ahuber.utils

sealed class Duration {
    data class Nanoseconds(override val value: Double): Duration()
    data class Milliseconds(override val value: Double): Duration()

    abstract val value: Double

    final override fun toString(): String {
        val unit = when (this) {
            is Nanoseconds -> "ns"
            is Milliseconds -> "ms"
        }

        return "%,.2f %s".format(value, unit)
    }
}

class TimedResult<T, TDuration: Duration> constructor(val duration: TDuration, val result: T)

fun Number.toNanoseconds() = Duration.Nanoseconds(this.toDouble())
fun Number.toMilliseconds() = Duration.Milliseconds(this.toDouble())

fun Duration.Nanoseconds.toMilliseconds() = Duration.Milliseconds(this.value / 1e+6)
fun Duration.Milliseconds.toNanoseconds() = Duration.Nanoseconds(this.value * 1e+6)

infix fun <T, TDuration: Duration> TDuration.withResult(result: T) = TimedResult(this, result)
infix fun <T, TDuration: Duration> T.withDuration(duration: TDuration) = duration withResult this

inline fun <T> measureNanoWithResult(block: () -> T): TimedResult<T, Duration.Nanoseconds> {
    val start = System.nanoTime()
    val result = block()
    val duration = System.nanoTime() - start
    return duration.toNanoseconds() withResult result
}

inline fun <T> measureMillisWithResult(block: () -> T): TimedResult<T, Duration.Milliseconds> {
    val
}