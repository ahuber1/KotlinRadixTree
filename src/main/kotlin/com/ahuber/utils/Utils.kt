package com.ahuber.utils

fun String.toCharArrayWithFill(newLength: Int, fillerChar: Char = '\u0000') : CharArray =
        CharArray(newLength) { index -> if (index < this.length) this[index] else fillerChar }



fun Double.format(numDigits: Int) : String {
    check(numDigits >= 0) { "numDigits must be greater than or equal to zero" }
    return String.format("%.${numDigits}f", this)
}

fun Float.format(numDigits: Int) : String = this.toDouble().format(numDigits)

inline val String.containsWhitespace: Boolean
    get() = any { it.isWhitespace() }

inline val Char.Companion.NULL_CHARACTER get() = '\u0000'