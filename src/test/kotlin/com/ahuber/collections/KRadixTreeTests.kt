package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.halveLeft
import com.ahuber.utils.halveRight
import com.ahuber.utils.middle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KRadixTreeTests {
    enum class Direction { LR, RL }

    data class SizedSequence<T>(val sequence: Sequence<T>, val size: Int): Sequence<T> {
        override fun iterator(): Iterator<T> = sequence.iterator()
    }

    @Test
    fun `test with words`() {
        val words = getWords(Direction.LR)
        val tree = KRadixTree()
        val targetWord = "abdications"
        var targetWordAdded = false

        for ((index, word) in words.withIndex()) {
            println("[${index + 1}/${words.size}] $word")

            if (targetWord in tree && !targetWordAdded) {
                fail("Something terrible happened.")
            }

            assertTrue(tree.add(word))
            assertTrue(word in tree)

            if (targetWord == word) {
                targetWordAdded = true
            }

            assertEquals(index + 1, tree.size)
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

            when (direction) {
                Direction.LR -> {
                    if (leftRange != null) generateIndices(leftRange, indices, level + 1, direction)
                    if (rightRange != null) generateIndices(rightRange, indices, level + 1, direction)
                }
                Direction.RL -> {
                    if (rightRange != null) generateIndices(rightRange, indices, level + 1, direction)
                    if (leftRange != null) generateIndices(leftRange, indices, level + 1, direction)
                }
            }
        }

        if (this.isEmpty()) {
            return emptyList()
        }

        return HashMap<Int, SortedSet<Int>>().let { map ->
            generateIndices(this.indices, map, 0, direction)
            map.asSequence().sortedBy { it.key }.flatMap { it.value.asSequence() }.distinct().toList()
        }
    }
}