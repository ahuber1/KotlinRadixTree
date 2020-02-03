package com.ahuber.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val String.containsWhitespace: Boolean
    get() = any { it.isWhitespace() }

inline val Char.Companion.NULL_CHARACTER get() = '\u0000'

inline val String.withQuotationMarks get() = "\"$this\""

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

fun <T> T.repeat(n: Int) = when (n <= 0) {
    true -> emptySequence()
    false -> (0 until n).asSequence().map { this }
}