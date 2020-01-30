package com.ahuber.utils

import java.util.*

typealias OptionPair<T1, T2> = Pair<Option<T1>, Option<T2>>

fun <T1, T2> Sequence<T1>.zipUneven(other: Sequence<T2>) =
        (this.iterator().zipUneven(other.iterator())).asSequence()

fun <T1, T2> Iterable<T1>.zipUneven(other: Iterable<T2>): Iterable<OptionPair<T1, T2>> {
    return object: Iterable<OptionPair<T1, T2>> {
        override fun iterator(): Iterator<OptionPair<T1, T2>> {
            return this@zipUneven.iterator().zipUneven(other.iterator())
        }
    }
}

fun <T1, T2> Iterator<T1>.zipUneven(other: Iterator<T2>) : Iterator<OptionPair<T1, T2>> =
        object: Iterator<OptionPair<T1, T2>> {
            override fun hasNext() = this@zipUneven.hasNext() || other.hasNext()

            override fun next(): OptionPair<T1, T2> {
                val item1 = this@zipUneven.mapNext { it }
                val item2 = other.mapNext { it }
                return OptionPair(item1, item2)
            }
        }

inline fun <T, R> Iterator<T>.mapNext(block: (Option<T>) -> R): R {
    val option = when (this.hasNext()) {
        true -> Option.Some(this.next())
        false -> Option.None<T>()
    }
    return block(option)
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

// Taken from: https://github.com/gazolla/Kotlin-Algorithm/blob/master/Shuffle/Shuffle.kt
fun <T : Comparable<T>> MutableList<T>.shuffle(): List<T>{
    val rg = Random()
    for (i in 0 until size) {
        val randomPosition = rg.nextInt(size)
        val tmp : T = this[i]
        this[i] = this[randomPosition]
        this[randomPosition] = tmp
    }
    return this
}