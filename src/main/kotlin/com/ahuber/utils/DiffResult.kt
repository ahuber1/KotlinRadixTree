package com.ahuber.utils

sealed class DiffResult {
    object Different: DiffResult()
    data class Shared(val sharedPrefix: String, val remainder: String): DiffResult()
    object Identical: DiffResult()
}

fun String.diffWith(other: String): DiffResult {
    if (this == other) {
        return DiffResult.Identical
    }

    var sharedPrefix = ""

    for (length in 1..this.length) {
        val substring = this.substring(0, length)

        if (other.startsWith(substring)) {
            sharedPrefix = substring
        } else if (sharedPrefix.isNotEmpty()) {
            break
        }
    }

    if (sharedPrefix.isEmpty()) {
        return DiffResult.Different
    }

    val remainder = this.substring(sharedPrefix.length)
    return DiffResult.Shared(sharedPrefix, remainder)
}

fun DiffResult.Shared.removeSharedPrefix(string: String): String? = when (string.startsWith(this.sharedPrefix)) {
    true -> string.substring(this.sharedPrefix.length)
    false -> null
}