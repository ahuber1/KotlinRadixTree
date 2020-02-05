package com.ahuber.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

inline val String.containsWhitespace: Boolean
    get() = any { it.isWhitespace() }

inline val Char.Companion.NULL_CHARACTER get() = '\u0000'

inline val Number.bd get() = BigDecimal(this.toDouble())

fun String.toCharArrayWithFill(newLength: Int, fillerChar: Char = '\u0000') : CharArray =
        CharArray(newLength) { index -> if (index < this.length) this[index] else fillerChar }

fun Double.format(numDigits: Int) : String {
    check(numDigits >= 0) { "numDigits must be greater than or equal to zero" }
    return String.format("%.${numDigits}f", this)
}

fun Float.format(numDigits: Int) : String = this.toDouble().format(numDigits)

fun <T : Any> T.getLogger(): Logger = LoggerFactory.getLogger(this.javaClass)

inline fun <reified T> getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun String.repeat(n: Int) = this.repeat<String>(n).joinToString("")

fun Char.repeat(n: Int) = this.repeat<Char>(n).joinToString("")

fun <T> T.repeat(n: Int) = when (n <= 0) {
    true -> emptySequence()
    false -> (0 until n).asSequence().map { this }
}

/**
 * Returns the length of this [IntRange]
 */
inline val IntRange.length get() = this.last - this.first + 1

/**
 * Returns the midpoint of this [IntRange]
 */
inline val IntRange.middle get() = (this.first + this.last) / 2

/**
 * Returns an [IntRange] that starts at the same start point as the received [IntRange]
 * and ends at the midpoint of the [IntRange] (exclusive)
 * @see [middle]
 */
fun IntRange.halveLeft(): IntRange? = when (this.length) {
    0 -> null
    else -> this.first until this.middle
}

/**
 * Returns an [IntRange] that starts at the midpoint of the received [IntRange] (inclusive) and ends at the
 * endpoint of the received [IntRange] (inclusive)
 */
fun IntRange.halveRight(): IntRange? = when (this.length) {
    0 -> null
    else -> this.middle until this.last
}