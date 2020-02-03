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

//// Taken from: https://github.com/gazolla/Kotlin-Algorithm/blob/master/Shuffle/Shuffle.kt
//fun <T : Comparable<T>> MutableList<T>.shuffle(): List<T>{
//    val rg = Random()
//    for (i in 0 until size) {
//        val randomPosition = rg.nextInt(size)
//        val tmp : T = this[i]
//        this[i] = this[randomPosition]
//        this[randomPosition] = tmp
//    }
//    return this
//}