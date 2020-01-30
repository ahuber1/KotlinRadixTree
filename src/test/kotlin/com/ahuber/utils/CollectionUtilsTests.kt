package com.ahuber.utils

import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

@Test
fun `test zipUneven using sequences`() {
    var sequence1 = sequenceOf(1, 2, 3, 4, 5)
    var sequence2 = sequenceOf(1, 2, 3, 4, 5)
    var combinedSequences = sequence1.zipUneven(sequence2)
    val count = combinedSequences.mapNotNull {
        val first = it.first as? Option.Some ?: return@mapNotNull null
        val second = it.second as? Option.Some ?: return@mapNotNull null
        return@mapNotNull first.value to second.value
    }.withIndex()
            .onEach { assertTrue(it.value.first == it.index + 1 && it.value.first == it.value.second) }
            .count()
    assertEquals(5, count)

    sequence1 = sequenceOf(1, 2, 3, 4, 5)
    sequence2 = sequenceOf(6, 7, 8, 9)
    combinedSequences = sequence1.zipUneven(sequence2)

    for ((index, pair) in combinedSequences.withIndex()) {
        val (first, second) = pair

        if (index == 4) {
            assertTrue(first is Option.Some && second is Option.None)
            assertEquals(5, (first as? Option.Some)?.value)
        } else {
            assertTrue(first is Option.Some && second is Option.Some)
            assertEquals(index + 1, (first as? Option.Some)?.value)
            assertEquals(index + 6, (second as? Option.Some)?.value)
        }
    }
}

@Test
fun `test zipUneven using iterables`() {
    var list1 = listOf(1, 2, 3, 4, 5)
    var list2 = listOf(1, 2, 3, 4, 5)
    var combinedIterables = list1.zipUneven(list2)
    val count = combinedIterables.mapNotNull {
        val first = it.first as? Option.Some ?: return@mapNotNull null
        val second = it.second as? Option.Some ?: return@mapNotNull null
        return@mapNotNull first.value to second.value
    }.withIndex()
            .onEach { assertTrue(it.value.first == it.index + 1 && it.value.first == it.value.second) }
            .count()
    assertEquals(5, count)

    list1 = listOf(1, 2, 3, 4, 5)
    list2 = listOf(6, 7, 8, 9)
    combinedIterables = list1.zipUneven(list2)

    for ((index, pair) in combinedIterables.withIndex()) {
        val (first, second) = pair

        if (index == 4) {
            assertTrue(first is Option.Some && second is Option.None)
            assertEquals(5, (first as? Option.Some)?.value)
        } else {
            assertTrue(first is Option.Some && second is Option.Some)
            assertEquals(index + 1, (first as? Option.Some)?.value)
            assertEquals(index + 6, (second as? Option.Some)?.value)
        }
    }
}