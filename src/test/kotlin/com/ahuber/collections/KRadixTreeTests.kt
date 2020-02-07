package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.test.*

typealias TestWithWordsBlock = (index: Int, word: String, set: MutableSet<String>) -> Unit

class KRadixTreeTests {
    enum class Direction { LR, RL }

    data class SizedSequence<T>(val sequence: Sequence<T>, val size: Int): Sequence<T> {
        override fun iterator(): Iterator<T> = sequence.iterator()
    }

    @Test
    fun `test with words`() {
        addThenRemove(Direction.LR)
        addThenRemove(Direction.RL)
    }

    @Test
    fun `test iterator`() {
        for (direction in Direction.values()) {
            val words = getWords(direction).toList()
            val treeSet = TreeSet<String>()
            treeSet.addAll(words)
            val radixTree = KRadixTree(words)
            assertEquals(treeSet.size, radixTree.size)

            for ((index, pair) in treeSet.zip(radixTree).withIndex()) {
                val (setString, treeString) = pair
                println("[$index] Set: \"$setString\", Tree: \"$treeString\"")
                assertEquals(setString, treeString)
            }
        }
    }

    @Test
    fun `test iterator with remove`() {
        val words = getWords().toList()
        val characterSet = words.asSequence()
                .groupBy { it.first() }
                .map { it.key to it.value.size }
                .asSequence()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
                .toSet()

        val controlSet = words.toSortedSet()
        val testSet = KRadixTree(words)

        val controlIterator = controlSet.iterator()
        val testIterator = testSet.iterator()

        val controlDestinationSet = TreeSet<String>()
        val testDestinationSet = TreeSet<String>()
        var index = 0

        while (controlIterator.hasNext() || testIterator.hasNext()) {
            println("index: ${index++}")
            val nextFromControl = controlIterator.next()
            val nextFromTest = testIterator.next()

            assertEquals(nextFromControl, nextFromTest)

            if (nextFromControl[0] !in characterSet) {
                assertTrue(controlDestinationSet.add(nextFromControl))
                assertTrue(testDestinationSet.add(nextFromTest))
            } else {
                controlIterator.remove()
                testIterator.remove()
            }
        }

        assertEquals(controlSet.size, controlDestinationSet.size)
        assertEquals(testSet.size, testDestinationSet.size)
        assertEquals(controlSet.size, testSet.size)

        val controlSequence = controlSet.asSequence()
        val testSequence = testSet.asSequence()
        val controlDestinationSequence = controlDestinationSet.asSequence()
        val testDestinationSequence = testDestinationSet.asSequence()
        val zippedSequences = controlSequence.zip(testSequence)
                .zip(controlDestinationSequence)
                .zip(testDestinationSequence)

        for (pair in zippedSequences) {
            val (pair1, testDestinationString) = pair
            val (pair2, controlDestinationString) = pair1
            val (controlString, testString) = pair2
            val allStrings = arrayOf(controlString, testString, controlDestinationString, testDestinationString)
            assertEquals(1, allStrings.distinct().size)
            assertTrue(allStrings.all { it[0] !in characterSet })
        }
    }

    private val Direction.otherDirection get() = when (this) {
        Direction.LR -> Direction.RL
        Direction.RL -> Direction.LR
    }

    private fun <T> Collection<T>.toSizedSequence() = SizedSequence(this.asSequence(), this.size)

    private fun addThenRemove(initialDirection: Direction) {
        var words = getWords(initialDirection)
        val sets: Array<MutableSet<String>> = arrayOf(HashSet(), KRadixTree())

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            if (index % 1000 == 0 && set is KRadixTree) {
                println("[ADDING] Index: $index, Word Count: ${words.size}")
            }

            assertTrue(set.add(word))
            assertTrue(word in set)
            assertEquals(index + 1, set.size)
        }

        words = getWords(initialDirection.otherDirection)
        val initialSize = words.size

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            try {
                if (index % 1000 == 0 && set is KRadixTree) {
                    println("[REMOVING] Index: $index, Word Count: ${words.size}")
                }

                if (index == words.size - 1) {
                    println("Something terrible is about to happen.")
                }

                assertTrue(set.remove(word))
                assertFalse(word in set)
                assertEquals(initialSize - index - 1, set.size)
            } catch (throwable: Throwable) {
                System.err.println("[REMOVING] Index: $index, Word Count: ${words.size}")
                throw throwable
            }
        }
    }

    private inline fun iterateThroughWordsAndSets(words: SizedSequence<String>,
            sets: Array<MutableSet<String>>, block: TestWithWordsBlock) {
        for ((index, word) in words.withIndex()) {
            for (set in sets) {
                block(index, word, set)
            }
        }
    }

    private fun getWords(): Sequence<String> {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        return file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .distinct()
    }

    private fun getWords(direction: Direction): SizedSequence<String> {
        var words: MutableList<String?> = getWords().toMutableList()

        val sequence = sequence<String> {
            while (true) {
                val indices = words.generateIndices(direction)

                if (indices.isEmpty()) {
                    break
                }

                for (index in indices) {
                    val word = words[index]
                    words[index] = null

                    if (word != null) {
                        yield(word)
                    }
                }

                words = words.filterNotNullTo(ArrayList())
            }
        }

        return SizedSequence(sequence, words.size)
    }

    private fun <T> List<T>.generateIndices(direction: Direction): List<Int> {
        fun generateIndices(range: IntRange, indices: MutableMap<Int, SortedSet<Int>>, level: Int, direction: Direction) {
            val set = indices.compute(level) { _, value -> value ?: TreeSet() }
            check(set != null)
            set.add(range.middle)

            val leftRange = range.halveLeft()
            val rightRange = range.halveRight()

            val (first, second) = when (direction) {
                Direction.LR -> leftRange to rightRange
                Direction.RL -> rightRange to leftRange
            }

            if (first != null) generateIndices(first, indices, level + 1, direction)
            if (second != null) generateIndices(second, indices, level + 1, direction)
        }

        return when (this.isEmpty()) {
            true -> emptyList()
            false -> {
                val map = HashMap<Int, SortedSet<Int>>()
                generateIndices(this.indices, map, 0, direction)
                map.asSequence().sortedBy { it.key }.flatMap { it.value.asSequence() }.distinct().toList()
            }
        }
    }
}