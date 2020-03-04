package com.ahuber.collections

import com.ahuber.test.utils.DataUtils
import com.ahuber.test.utils.Direction
import com.ahuber.test.utils.assertContainsWords
import com.ahuber.test.utils.oppositeDirection
import org.junit.jupiter.api.Assertions
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class WordSearchTreeTests<TSet> where TSet : MutableSet<String>, TSet : WordSearchTree {
    protected abstract fun createEmptySet(): TSet

    protected open fun createSetWith(elements: Sequence<String>): TSet {
        val set = createEmptySet()
        set.addAll(elements)
        return set
    }

    protected open fun createSetWith(elements: Iterable<String>): TSet {
        val set = createEmptySet()
        set.addAll(elements)
        return set
    }

    protected open fun createSetWith(vararg elements: String): TSet {
        val set = createEmptySet()
        set.addAll(elements)
        return set
    }

    @Test
    fun `test with words`() {
        for (initialDirection in Direction.values()) {
            var words = DataUtils.getWords(initialDirection)
            val sets: Array<MutableSet<String>> = arrayOf(HashSet(), createEmptySet())
            for ((index, word) in words.withIndex()) {
                for (set in sets) {
                    assertTrue(set.add(word))
                    assertTrue(word in set)
                    assertEquals(index + 1, set.size)
                }
            }
            words = DataUtils.getWords(initialDirection.oppositeDirection)
            val initialSize = words.size
            for ((index, word) in words.withIndex()) {
                for (set in sets) {
                    assertTrue(set.remove(word))
                    assertFalse(word in set)
                    assertEquals(initialSize - index - 1, set.size)
                }
            }
        }
    }

    @Test
    fun `test iterator`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(60)) {
            for (direction in Direction.values()) {
                val words = DataUtils.getWords(direction).toList()
                val wordTree = createSetWith(words)
                wordTree.assertContainsWords(words)
            }
        }
    }

    @Test
    fun `test iterator with remove`() {
        Assertions.assertTimeoutPreemptively(Duration.ofMinutes(1)) {
            val words = DataUtils.getWords()
            val characterSet = words.groupBy { it.first() }
                    .map { it.key to it.value.size }
                    .asSequence()
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { it.first }
                    .toSet()
            val controlSet = words.toMutableSet()
            val testSet = createSetWith(words)

            val controlIterator = controlSet.iterator()
            val testIterator = testSet.iterator()
            var index = -1L
            var removeFromControlCount = 0
            var removeFromTestCount = 0

            while (controlIterator.hasNext() || testIterator.hasNext()) {
                index++
                try {
                    val nextFromControl = controlIterator.next()
                    val nextFromTest = testIterator.next()

                    if (nextFromControl[0] in characterSet) {
                        controlIterator.remove()
                        removeFromControlCount++
                    }

                    if (nextFromTest[0] in characterSet) {
                        testIterator.remove()
                        removeFromTestCount++
                    }
                } catch (e: NoSuchElementException) {
                    System.err.apply {
                        println("NoSuchElementException")
                        printf("\tIndex: %,d\n", index)
                        println("\tRemove Counts:")
                        printf("\t\tControl: %,d\n", removeFromControlCount)
                        printf("\t\t   Test: %,d\n", removeFromTestCount)
                    }
                    throw e
                }
            }

            assertTrue(controlSet.none { it.first() in characterSet })
            assertTrue(testSet.none { it.first() in characterSet })
            assertEquals(removeFromControlCount, removeFromTestCount)
            assertEquals(controlSet.size, testSet.size)
            testSet.assertContainsWords(controlSet)
        }
    }

    @Test
    fun `test search`() {
        val tree = createSetWith("apple", "application", "apply")
        val result = tree.search("apple")
        assertTrue(result is WordSearchResult.ExactMatch, "'result' should be of type " +
                "${WordSearchResult.ExactMatch::class.java.typeName}, but it is of type ${result.javaClass.typeName}")
        assertEquals("apple", result.searchString)
        assertTrue(result.longerWords.isEmpty())
    }
}