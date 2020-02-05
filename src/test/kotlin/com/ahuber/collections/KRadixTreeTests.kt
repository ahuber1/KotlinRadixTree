package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.system.measureNanoTime
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

    private val Direction.otherDirection get() = when (this) {
        Direction.LR -> Direction.RL
        Direction.RL -> Direction.LR
    }

    private fun addThenRemove(initialDirection: Direction) {
        var words = getWords(initialDirection)
        val sets: Array<MutableSet<String>> = arrayOf(HashSet(), KRadixTree())
        val stringLength = "%,d".format(words.size).length

        var addDurationSum = 0.0.bd
        var containsDurationSum = 0.0.bd

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            var indentation: String? = null

            if (set is KRadixTree) {
                val prefix = "[%,${stringLength}d of %,${stringLength}d]".format(index + 1, words.size)
                println("$prefix Adding elements...")
                indentation = ' '.repeat(prefix.length)
            }

            val addDuration = measureMillisWithResult { set.add(word) }.also { assertTrue(it.result) }.duration.toMillis()

            if (indentation != null) {
                addDurationSum += addDuration.bd
                println("%s Add took %,d milliseconds to run.".format(indentation, addDuration))
                println("%s Add has averaged %,.2f nanoseconds so far.".format(indentation, addDurationSum / (index + 1).bd))
            }

            val containsDuration = measureMillisWithResult { word in set }.also { assertTrue(it.result) }.duration.toMillis()

            if (indentation != null) {
                containsDurationSum += containsDuration.bd
                println("%s Contains took %,d milliseconds to run.".format(indentation, containsDuration))
                println("%s Contains has averaged %,.2f nanoseconds so far.".format(indentation, containsDurationSum / (index + 1).bd))
            }

            assertEquals(index + 1, set.size)
        }

        words = getWords(initialDirection.otherDirection)
        val initialSize = words.size

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            println("[${index + 1}/${words.size}] $word: Removing elements from a ${set.javaClass.typeName}")
            assertTrue(set.remove(word))
            assertFalse(word in set)
            assertEquals(initialSize - index - 1, set.size)
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

    private fun getWords(direction: Direction): SizedSequence<String> {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        var words: MutableList<String?> = file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()

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