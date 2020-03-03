package com.ahuber.utils

import kotlin.math.min

sealed class DiffResult {
    object Different: DiffResult()
    data class Shared(val sharedPrefix: String, val remainder: String): DiffResult()
    object Identical: DiffResult()
}

fun String.diffWith(other: String): DiffResult {
    val minLength = min(this.length, other.length)
    var endIndex = -1

    for (index in 0 until minLength) {
        if (this[index] != other[index]) break
        endIndex = index
    }

    return when {
        endIndex == -1 -> DiffResult.Different
        this.length == other.length && endIndex == this.lastIndex -> DiffResult.Identical
        else -> DiffResult.Shared(this.substring(0..endIndex), this.substring(endIndex + 1))
    }
}

fun DiffResult.Shared.removeSharedPrefix(string: String): String? = when (string.startsWith(this.sharedPrefix)) {
    true -> string.substring(this.sharedPrefix.length)
    false -> null
}